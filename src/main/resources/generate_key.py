import uuid, os, hashlib

kid = 'k' + uuid.uuid4().hex[:12]
secret = os.urandom(32).hex()
hashed = hashlib.sha256(secret.encode('utf-8')).hexdigest()

print('KID:', kid)
print('SECRET:', secret)
print('TOKEN:', f"{kid}.{secret}")
print('SHA256(secret):', hashed)
