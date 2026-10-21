const express = require('express');
const sqlite3 = require('sqlite3').verbose();
const jwt = require('jsonwebtoken');
const bodyParser = require('body-parser');
const cors = require('cors');

const app = express();
const db = new sqlite3.Database('./mechewallet.db');
const JWT_SECRET = 'SECRET';
let processingTransactions = {};


app.use(cors());
app.use(bodyParser.json());


db.serialize(() => {
  db.run(`CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    username TEXT NOT NULL UNIQUE,
    email TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    balance REAL DEFAULT 1000,
    is_merchant INTEGER DEFAULT 0
  )`);

  db.run(`CREATE TABLE IF NOT EXISTS transactions (
    transaction_id INTEGER PRIMARY KEY AUTOINCREMENT,
    sender_id TEXT,
    receiver_id TEXT,
    amount REAL,
    timestamp TEXT,
    refunded INTEGER DEFAULT 0,
    FOREIGN KEY(sender_id) REFERENCES users(id),
    FOREIGN KEY(receiver_id) REFERENCES users(id)
  )`);

});


const protect = (req, res, next) => {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];
  if (!token) return res.status(401).send({ message: 'No autorizado, no hay token' });

  jwt.verify(token, JWT_SECRET, (err, user) => {
    if (err) return res.status(403).send({ message: 'Token inválido' });
    req.user = user;
    next();
  });
};


app.post('/api/register', (req, res) => {
  const { username, email, password, is_merchant } = req.body;
  const id = new Date().getTime().toString();


  let normalizedEmail = email;
  let flag = null;

  if (email.includes('%00')) {
    normalizedEmail = email.split('%00')[0];
    flag = "FLAG5{NULL_BYTE_1NJECT1ON_TR1GGERED}";
  } else if (email.includes('\u0000')) {
    normalizedEmail = email.split('\u0000')[0];
    flag = "FLAG5{NULL_BYTE_1NJECT1ON_TR1GGERED}";
  }


  db.get('SELECT * FROM users WHERE email = ?', [normalizedEmail], (err, existingUser) => {
    if (err) return res.status(500).send({ message: 'Error al consultar usuario' });

    if (existingUser) {
      if (!flag) {
        
        return res.status(400).send({ message: 'Usuario o correo ya existe' });
      }

      db.run(
        'UPDATE users SET password = ?, username = ? WHERE id = ?',
        [password, username, existingUser.id],
        function(err) {
          if (err) return res.status(500).send({ message: 'Error al actualizar usuario' });

          const token = jwt.sign({ id: existingUser.id, username }, JWT_SECRET);
          const response = {
            message: 'Usuario actualizado (null byte attack)',
            userId: existingUser.id,
            token,
            flag
          };
          res.status(200).send(response);
        }
      );
    } else {
      
      db.run(
        'INSERT INTO users (id, username, email, password, is_merchant) VALUES (?, ?, ?, ?, ?)',
        [id, username, normalizedEmail, password, is_merchant ? 1 : 0],
        function(err) {
          if (err) return res.status(400).send({ message: 'Error de registro' });

          const token = jwt.sign({ id, username }, JWT_SECRET);
          const response = {
            message: 'Usuario registrado',
            userId: id,
            token
          };
          if (flag) response.flag = flag;

          res.status(201).send(response);
        }
      );
    }
  });
});




app.post('/api/login', (req, res) => {
  const { email, password } = req.body;

  db.get(
    `SELECT * FROM users WHERE email = ? AND password = ?`,
    [email, password],
    (err, user) => {
      if (err || !user) return res.status(401).send({ message: 'Credenciales inválidas.' });

      const token = jwt.sign({ id: user.id, username: user.username }, JWT_SECRET);
      res.send({
        message: 'Inicio de sesión exitoso.',
        userId: user.id,
        is_merchant: user.is_merchant === 1,
        token,
      });
    }
  );
});


app.get('/api/profile/:userId', protect, (req, res) => {
  const { userId } = req.params;
  db.get(`SELECT username, balance, email FROM users WHERE id = ?`, [userId], (err, user) => {
    if (err || !user) return res.status(404).send({ message: 'Usuario no encontrado.' });
    res.send(user);
  });
});


app.post('/api/generate-qr', protect, (req, res) => {
  const { merchant_id, amount } = req.body;
  if (!merchant_id || !amount) return res.status(400).send({ message: 'Se requiere ID de comercio y monto.' });

  res.send({ type: 'payment', receiver_id: merchant_id, amount: parseFloat(amount) });
});


app.post('/api/transfer', protect, (req, res) => {
  const { sender_id, receiver_id, amount } = req.body;
  let awardedFlags = [];

  db.serialize(() => {
    db.run('BEGIN TRANSACTION;');

    db.get('SELECT balance FROM users WHERE id = ?', [sender_id], (err, sender) => {
      if (err || !sender || sender.balance < amount) {
        db.run('ROLLBACK;');
        return res.status(400).send({ message: 'Saldo insuficiente o usuario no encontrado.' });
      }

      db.run('UPDATE users SET balance = balance - ? WHERE id = ?', [amount, sender_id], (err) => {
        if (err) { db.run('ROLLBACK;'); return res.status(500).send({ message: 'Error al actualizar remitente.' }); }

        db.run('UPDATE users SET balance = balance + ? WHERE id = ?', [amount, receiver_id], (err) => {
          if (err) { db.run('ROLLBACK;'); return res.status(500).send({ message: 'Error al actualizar destinatario.' }); }

          const timestamp = new Date().toISOString();

          db.run(`INSERT INTO transactions (sender_id, receiver_id, amount, timestamp) VALUES (?, ?, ?, ?)`,
            [sender_id, receiver_id, amount, timestamp],
            function(err) {
              if (err) { db.run('ROLLBACK;'); return res.status(500).send({ message: 'Error al registrar la transacción.' }); }

              db.run('COMMIT;', (commitErr) => {
                if (commitErr) return res.status(500).send({ message: 'Error al confirmar la transacción.' });

                
                if (req.user.id !== sender_id) {
                  awardedFlags.push('FLAG2{m1_0th3r_acc0unt_1s_y0ur_acc0unt}');
                }

                
                if (req.body.amount < 0) {
                  awardedFlags.push('FLAG3{n3gat1v3_1n_th3_h0us3}');
                }
                res.send({
                  message: 'Transferencia completada exitosamente.',
                  transaction_id: this.lastID,
                  sender_id,
                  receiver_id,
                  amount,
                  timestamp,
                  refunded: 0,
                  flags: awardedFlags
                });
              });
            }
          );
        });
      });
    });
  });
});

app.get('/api/transactions/:userId', protect, (req, res) => {
  const { userId } = req.params;
  db.all(`SELECT
            t.transaction_id,
            t.amount,
            t.timestamp,
            t.refunded,
            s.username AS sender_username,
            r.username AS receiver_username,
            r.id AS receiver_id
          FROM transactions t
          JOIN users s ON t.sender_id = s.id
          JOIN users r ON t.receiver_id = r.id
          WHERE t.sender_id = ? OR t.receiver_id = ?
          ORDER BY t.timestamp DESC`,
    [userId, userId], (err, rows) => {
      if (err) return res.status(500).send({ message: 'Error al obtener transacciones.' });
      res.send(rows);
    });
});

app.post('/refund', protect, (req, res) => {
  const { transaction_id } = req.body;
  let awardedFlags = [];

  if (!transaction_id) return res.status(400).send({ message: 'Se requiere transaction_id' });

  db.get('SELECT * FROM transactions WHERE transaction_id = ?', [transaction_id], (err, transaction) => {
    if (err || !transaction) return res.status(404).send({ message: 'Transacción no encontrada' });


    if (req.user.id !== transaction.receiver_id) {
      awardedFlags.push('FLAG4{r3fUnd_1d0rs_3v3r1wh3r3}');
    }

    if (transaction.refunded === 1) {
      return res.status(400).send({ message: 'La transacción ya fue devuelta', flags: awardedFlags });
    }

    if (processingTransactions[transaction_id]) {
      awardedFlags.push('FLAG6{rac3_c0nd1t10n_3v3r1wh3r3}');
      return res.status(400).send({ message: 'Transacción en proceso por otro request', flags: awardedFlags });
    }

    processingTransactions[transaction_id] = true;

    db.get('SELECT balance FROM users WHERE id = ?', [transaction.sender_id], (err, sender) => {
      if (err || !sender) {
        delete processingTransactions[transaction_id];
        return res.status(400).send({ message: 'Remitente no encontrado' });
      }

      db.get('SELECT balance FROM users WHERE id = ?', [transaction.receiver_id], (err, receiver) => {
        if (err || !receiver) {
          delete processingTransactions[transaction_id];
          return res.status(400).send({ message: 'Receptor no encontrado' });
        }

        
        db.run('UPDATE users SET balance = ? WHERE id = ?', [sender.balance + transaction.amount, transaction.sender_id]);
        db.run('UPDATE users SET balance = ? WHERE id = ?', [receiver.balance - transaction.amount, transaction.receiver_id]);
        db.run('UPDATE transactions SET refunded = 1 WHERE transaction_id = ?', [transaction_id], () => {
          
          delete processingTransactions[transaction_id];

          res.send({
            message: 'Devolución enviada',
            refunded: 1,
            flags: awardedFlags
          });
        });
      });
    });
  });
});

const PORT = 2095;
const HOST = "0.0.0.0";
app.listen(PORT, HOST, () => console.log(`Servidor de CTF corriendo en el puerto ${PORT}`));
