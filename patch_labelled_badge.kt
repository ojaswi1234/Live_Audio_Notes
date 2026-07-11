import java.io.File

fun main() {
    val file = File("app/src/main/java/com/example/ui/screens/SessionScreen.kt")
    var content = file.readText()
    
    val badgeRegex = """@Composable\s*fun LabelledBadge\(label: String, icon: ImageVector\) \{\s*Row\(\s*verticalAlignment = Alignment\.CenterVertically,\s*horizontalArrangement = Arrangement\.spacedBy\(6\.dp\),\s*modifier = Modifier\s*\.clip\(RoundedCornerShape\(8\.dp\)\)\s*\.background\(MaterialTheme\.colorScheme\.surface\.copy\(alpha = 0\.6f\)\)\s*\.padding\(horizontal = 10\.dp, vertical = 6\.dp\)\s*\) \{\s*Icon\(icon, contentDescription = null, tint = MaterialTheme\.colorScheme\.primary, modifier = Modifier\.size\(16\.dp\)\)\s*Text\(\s*label,\s*style = MaterialTheme\.typography\.bodySmall\.copy\(fontWeight = FontWeight\.SemiBold\),\s*color = MaterialTheme\.colorScheme\.onSurfaceVariant\s*\)\s*\}\s*\}""".toRegex()
    
    val replacement = """@Composable
fun LabelledBadge(label: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}"""

    val newContent = content.replace(badgeRegex, replacement)
    file.writeText(newContent)
}
