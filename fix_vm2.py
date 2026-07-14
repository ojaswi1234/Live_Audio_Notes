import sys

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'r') as f:
    text = f.read()

target = """            } finally {
                isClubTyping = false
            }
        }
    }
}"""
replacement = """            } finally {
                isClubTyping = false
            }
        }
    }"""
text = text.replace(target, replacement)

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'w') as f:
    f.write(text)
