# How to create a localhost SSL certificate for developing and testing:

## 1. Instrallng mkcert 

### Windows:

```bash 
choco install mkcert 
# or
scoop install mkcert
```

### Macos:

```bash 
brew install mkcert
```

## 2. Adding all your local browsers to mkcert:
 
```bash
mkcert -install
```

## 3. Create a localhost key for **Frontend**:

Run it inside the **frontend** folder (the files go to `frontend/certs/`):

```bash 
mkdir -p certs
mkcert -cert-file ./certs/localhost+2.pem -key-file ./certs/localhost+2-key.pem localhost 127.0.0.1
```


## 4. Create a localhost key for **Backend**:

Run it inside the **backend** folder (the file goes to `backend/.certs/`):

```bash
mkcert -pkcs12 -p12-file ./.certs/localhost+1.p12 localhost 127.0.0.1
```

## 5. Environment files

The `.env` files are not in the repository. Each person creates them:

- **Frontend** `.env` (copy of `.env.example`):

  ```
  VITE_API_BASE_URL=https://localhost:8443
  ```

- **Backend** `.env` (ask the team for the Stripe key, never upload it):

  ```
  STRIPE_API_KEY=sk_test_...
  STRIPE_SUCCESS_URL=https://localhost:5173/success
  STRIPE_CANCEL_URL=https://localhost:5173/cancel
  ```

Start the backend first, then `npm run dev`, and open `https://localhost:5173`.

## If Windows blocks mkcert ("Permission denied" / "Application Control policy")

Run `mkcert -install` once (if it is blocked too, ask a teammate or allow mkcert in Windows Security). Then create the certificates with OpenSSL (included in Git Bash), signed by the local mkcert CA:

```bash
CA="$LOCALAPPDATA/mkcert"
printf "basicConstraints=CA:FALSE\nkeyUsage=digitalSignature,keyEncipherment\nextendedKeyUsage=serverAuth\nsubjectAltName=DNS:localhost,IP:127.0.0.1,IP:::1\n" > ext.cnf

# Frontend (inside the frontend folder)
mkdir -p certs
MSYS_NO_PATHCONV=1 openssl req -new -newkey rsa:2048 -nodes -keyout certs/localhost+2-key.pem -out req.csr -subj "/O=mkcert development certificate/CN=localhost"
openssl x509 -req -in req.csr -CA "$CA/rootCA.pem" -CAkey "$CA/rootCA-key.pem" -CAcreateserial -CAserial ca.srl -out certs/localhost+2.pem -days 825 -sha256 -extfile ext.cnf

# Backend (inside the backend folder: first run again the CA= and printf lines from above)
MSYS_NO_PATHCONV=1 openssl req -new -newkey rsa:2048 -nodes -keyout key.pem -out req.csr -subj "/O=mkcert development certificate/CN=localhost"
openssl x509 -req -in req.csr -CA "$CA/rootCA.pem" -CAkey "$CA/rootCA-key.pem" -CAcreateserial -CAserial ca.srl -out cert.pem -days 825 -sha256 -extfile ext.cnf
openssl pkcs12 -export -in cert.pem -inkey key.pem -out .certs/localhost+1.p12 -passout pass:changeit

rm -f req.csr ca.srl key.pem cert.pem ext.cnf
```

# If you have an official certificate you can put the path into ENVIRONMENT VARIABLE

```bash
export SSL_KEYSTORE_PATH = ".certs/keystore.p12"
export SSL_KEYSTORE_PASSWORD = "changeit"
```