import sys

with open('gradle/libs.versions.toml', 'r') as f:
    text = f.read()

target = 'firebase-auth = { group = "com.google.firebase", name = "firebase-auth" }'
replacement = target + '\nfirebase-messaging = { group = "com.google.firebase", name = "firebase-messaging" }'

text = text.replace(target, replacement)

with open('gradle/libs.versions.toml', 'w') as f:
    f.write(text)
