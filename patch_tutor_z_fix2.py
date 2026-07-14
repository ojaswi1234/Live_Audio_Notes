import sys

with open('app/src/main/java/com/example/ui/components/TutorOverlay.kt', 'r') as f:
    text = f.read()

target_import = "import androidx.compose.ui.unit.sp"
replacement_import = "import androidx.compose.ui.unit.sp\nimport androidx.compose.ui.zIndex.zIndex"
text = text.replace(target_import, replacement_import)

target_modifier = """    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(alpha = 0.99f)
    ) {"""
replacement_modifier = """    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f)
            .graphicsLayer(alpha = 0.99f)
    ) {"""
text = text.replace(target_modifier, replacement_modifier)

with open('app/src/main/java/com/example/ui/components/TutorOverlay.kt', 'w') as f:
    f.write(text)
