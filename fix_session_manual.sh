# Remove the first actions block entirely (lines 200 to 206 roughly)
sed -i '/actions = {/,/},/!b;//{/title = {/!{/actions = {/!{/}/!d}}};/actions = {/d;/},/d' app/src/main/java/com/example/ui/screens/SessionScreen.kt
