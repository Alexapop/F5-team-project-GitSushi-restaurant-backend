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

## 3. Create a localhost key for Backend:

```bash
mkcert -pkcs12 -p12-file ./.certs/localhost+1.p12 localhost 127.0.0.1;
```

# If you have an official certificate you can put the path into ENVIRONMENT VARIABLE

```bash
export SSL_KEYSTORE_PATH = ".certs/keystore.p12"
export SSL_KEYSTORE_PASSWORD = "changeit"
```