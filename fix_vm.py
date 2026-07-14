import sys

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'r') as f:
    text = f.read()

target = """            } finally {
                isClubTyping = false
            }
        }
    }
}

    // --- Authentication ---"""

replacement = """            } finally {
                isClubTyping = false
            }
        }
    }

    // --- Authentication ---"""

text = text.replace(target, replacement)

# Now we need to append the closing brace for the class before `data class ClubMessage`
target2 = """}
data class ClubMessage"""
replacement2 = """}

data class ClubMessage"""
text = text.replace(target2, replacement2)

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'w') as f:
    f.write(text)
