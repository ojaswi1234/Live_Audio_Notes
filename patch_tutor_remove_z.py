import sys

with open('app/src/main/java/com/example/ui/components/TutorOverlay.kt', 'r') as f:
    text = f.read()

target = "import androidx.compose.ui.zIndex.zIndex\n"
text = text.replace(target, "")

target2 = "            .zIndex(100f)\n"
text = text.replace(target2, "")

with open('app/src/main/java/com/example/ui/components/TutorOverlay.kt', 'w') as f:
    f.write(text)
