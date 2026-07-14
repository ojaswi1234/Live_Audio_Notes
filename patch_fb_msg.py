import sys

with open('app/src/main/java/com/example/network/FirebaseManager.kt', 'r') as f:
    text = f.read()

target = "ClubMessage(sender = sender, text = text, persona = persona)"
replacement = "ClubMessage(java.util.UUID.randomUUID().toString(), sender, text, System.currentTimeMillis())"
text = text.replace(target, replacement)

with open('app/src/main/java/com/example/network/FirebaseManager.kt', 'w') as f:
    f.write(text)
