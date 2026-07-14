import sys

with open('app/src/main/java/com/example/ui/components/TutorOverlay.kt', 'r') as f:
    text = f.read()

target = """    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(alpha = 0.99f)
    ) {"""

replacement = """    Box(
        modifier = Modifier
            .fillMaxSize()
            .androidx.compose.ui.zIndex.zIndex(100f)
            .graphicsLayer(alpha = 0.99f)
    ) {"""
text = text.replace(target, replacement)

with open('app/src/main/java/com/example/ui/components/TutorOverlay.kt', 'w') as f:
    f.write(text)
