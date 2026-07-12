sed -i '221,228c\
                    actions = {\
                        IconButton(onClick = { viewModel.navigateTo(AppScreen.BOOK_CLUB) }) {\
                            Icon(Icons.Default.Group, contentDescription = "Book Club")\
                        }\
                        IconButton(onClick = {\
                            coroutineScope.launch {\
                                drawerState.open()\
                            }\
                        }) {' app/src/main/java/com/example/ui/screens/SessionScreen.kt
