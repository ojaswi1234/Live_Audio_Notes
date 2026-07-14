import sys

with open('app/src/main/AndroidManifest.xml', 'r') as f:
    text = f.read()

target = "    </application>"
replacement = """
        <!-- Firebase Cloud Messaging Service -->
        <service
            android:name=".network.EchoReaderMessagingService"
            android:exported="true">
            <intent-filter>
                <action android:name="com.google.firebase.MESSAGING_EVENT" />
            </intent-filter>
        </service>
    </application>"""

text = text.replace(target, replacement)

with open('app/src/main/AndroidManifest.xml', 'w') as f:
    f.write(text)
