import sys

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'r') as f:
    text = f.read()

text = text.replace(".kotlinx.coroutines.tasks.await()", ".await()")
text = text.replace("repository.userStats.kotlinx.coroutines.flow.firstOrNull()", "repository.userStats.firstOrNull()")

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'w') as f:
    f.write(text)
