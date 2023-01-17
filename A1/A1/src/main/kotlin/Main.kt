import javafx.application.Application
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.layout.BorderPane
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.text.*
import javafx.stage.Stage
import java.io.File
import java.io.FileInputStream

class Main : Application() {
    // Generate variables to contain file information
    var currentFileName: String = ""
    var fileList = ListView<String>()
    var dir = File("${System.getProperty("user.dir")}\\test")
    var isHidden:Boolean = true

    val buttonMinWidth = 50.0
    val buttonPrefWidth = 100.0
    val buttonMaxWidth = 200.0
    override fun start(stage: Stage) {
        // initialize the list of file using the root directory
        updateFileList(isHidden)

        // Create stack pane at the center of the border pane to contain image
        val stackpane = StackPane()
        stackpane.minHeight = 0.0
        stackpane.minWidth = 0.0

        // Display the path of the current file at the bottom left of the border pane
        val statusbar = VBox()
        statusbar.children.add(Text(dir.absolutePath))

        // Start the file list with selecting the first file and set the backgroud color
        fileList.selectionModel.selectIndices(0)
        displayContent(stackpane, statusbar)
        fileList.style = "-fx-background-color: white;"

        // Set up the file list mouse click and key pressed event, so that they will handle
        //   different feature
        fileList.setOnMouseClicked { event ->
            displayContent(stackpane, statusbar)

            if (event.clickCount == 2) {
                goInDirectory(stackpane, statusbar)
            }
        }

        fileList.setOnKeyPressed { event ->
            if (event.code == KeyCode.ENTER) {
                goInDirectory(stackpane, statusbar)
            } else if (event.code == KeyCode.BACK_SPACE) {
                goOutDirectory(stackpane, statusbar)
            } else if (event.code == KeyCode.UP) {
                val newIndex = fileList.selectionModel.selectedIndex
                fileList.selectionModel.selectIndices(newIndex)
                displayContent(stackpane, statusbar)
            } else if (event.code == KeyCode.DOWN) {
                val newIndex = fileList.selectionModel.selectedIndex
                fileList.selectionModel.selectIndices(newIndex)
                displayContent(stackpane, statusbar)
            }
        }

        // Generate menubar
        val menuBar = MenuBar()
        val fileMenu = Menu("File")
        val viewMenu = Menu("View")
        val menuActions = Menu("Actions")
        val menuOptions = Menu("Options")

        // The menu item for File
        val menuHome = MenuItem("Home")
        val menuOpen = MenuItem("Open")
        val menuPrev = MenuItem("Prev")
        val menuClose = MenuItem("Close")
        val menuQuit = MenuItem("Quit")
        menuHome.setOnAction { goHomeDirectory(stackpane, statusbar) }
        menuOpen.setOnAction { goInDirectory(stackpane, statusbar) }
        menuPrev.setOnAction { goOutDirectory(stackpane, statusbar) }
        menuClose.setOnAction { stackpane.children.clear() }
        menuQuit.setOnAction { Platform.exit() }

        // The menu item for View
        val menuReflect = MenuItem("Reflect files")
        val menuHide = MenuItem("Hidden files")
        menuReflect.setOnAction { reflectHiddenFile(stackpane, statusbar, true) }
        menuHide.setOnAction { reflectHiddenFile(stackpane, statusbar, false) }

        // The menu item for Actions
        val menuRename = MenuItem("Rename")
        val menuMove = MenuItem("Move")
        val menuDelete = MenuItem("Delete")
        menuRename.setOnAction { renameCurrentFile(stackpane, statusbar) }
        menuMove.setOnAction { moveFileDirectory(stackpane, statusbar) }
        menuDelete.setOnAction { deleteCurrentFile(stackpane, statusbar) }

        // The menu item for Options
        val menuShow = MenuItem("Show Hidden Files")
        menuShow.setOnAction {
            isHidden = !isHidden

            // If the current pointing file is hiddenm, then we clear the stackpane
            val currentFile = File(dir.absolutePath + "\\$currentFileName")
            if (isHidden && currentFile.name.startsWith(".")) {
                stackpane.children.clear()
            }

            updateFileList(isHidden)
        }

        // Add menu item to their corresponding menus
        fileMenu.items.addAll(menuHome, menuOpen, menuPrev, menuClose, menuQuit)
        viewMenu.items.addAll(menuReflect, menuHide)
        menuActions.items.addAll(menuRename, menuMove, menuDelete)
        menuOptions.items.addAll(menuShow)
        menuBar.menus.addAll(fileMenu, viewMenu, menuActions, menuOptions)

        // Generate the toolbar
        val toolbar = ToolBar()
        val homeButton: Button = StandardButton("HOME")
        homeButton.setOnAction {
            // If the current directory is not the root directory, we go to the root directory
            goHomeDirectory(stackpane, statusbar)
        }

        val prevButton: Button = StandardButton("Prev")
        prevButton.setOnAction {
            // if the current directory is not the root directory, we go to the parent directory
            goOutDirectory(stackpane, statusbar)
        }

        val nextButton: Button = StandardButton("Next")
        nextButton.setOnAction {
            // if the current file is a directory, then we navigate into the current directory, if not,
            //   we do nothing
            goInDirectory(stackpane, statusbar)
        }

        val deleteButton: Button = StandardButton("Delete")
        deleteButton.setOnAction {
            deleteCurrentFile(stackpane, statusbar)
        }

        val renameButton: Button = StandardButton("Rename")
        renameButton.setOnAction {
            renameCurrentFile(stackpane, statusbar)
        }

        val moveButton: Button = StandardButton("Move")
        moveButton.setOnAction {
            moveFileDirectory(stackpane, statusbar)
        }

        toolbar.items.addAll(homeButton, prevButton, nextButton, deleteButton, renameButton, moveButton)
        toolbar.padding = Insets(10.0)

        // stack menu and toolbar in the top region
        val vbox = VBox(menuBar, toolbar)

        // SETUP LAYOUT
        val border = BorderPane()
        border.top = vbox
        border.left = fileList
        border.center = stackpane
        border.bottom = statusbar

        // CREATE AND SHOW SCENE
        val scene = Scene(border, 800.0, 600.0)
        stage.scene = scene
        stage.title = "File Browser"
        stage.show()
    }

    // Customized button
    // Used to set MIN, MAX, and PREFERRED sizes for all buttons
    private inner class StandardButton internal constructor(caption: String? = "Untitled") :
        Button(caption) {
        init {
            isVisible = true
            minWidth = buttonMinWidth
            prefWidth = buttonPrefWidth
            maxWidth = buttonMaxWidth
        }
    }

    // The function will update the recent file list
    fun updateFileList(isHidden: Boolean) {
        fileList.items.clear()
        dir.listFiles().forEach {
            if (isHidden) {
                if (!it.name.startsWith(".")) {
                    if (it.isDirectory) {
                        fileList.items.add( it.name + "\\")
                    } else {
                        fileList.items.add( it.name )
                    }
                }
            } else {
                if (it.isDirectory) {
                    fileList.items.add( it.name + "\\")
                } else {
                    fileList.items.add( it.name )
                }
            }
        }
    }

    // The function will remove everything from the stack pane
    fun showNothing(stackpane: StackPane) {
        stackpane.children.clear()
    }

    // The function will first clear the given stack pane and then display the image
    //   within the provided file
    fun displayImage(stackpane: StackPane, currentFile: File) {
        stackpane.children.clear()
        val stream = FileInputStream(currentFile.absoluteFile)
        val imageView = ImageView(Image(stream))
        stream.close()
        imageView.isPreserveRatio = true
        imageView.fitWidthProperty().bind(stackpane.widthProperty())
        imageView.fitHeightProperty().bind(stackpane.heightProperty())
        stackpane.children.add(imageView)
        StackPane.setAlignment(imageView,Pos.CENTER)
    }

    // The function will also clear the given stack pane for the first time, and then
    //   the function will display the text contents inside the given file and provide
    //   scrollbar if possible
    fun displayTxt(stackpane: StackPane, currentFile: File) {
        stackpane.children.clear()

        // Generate text view to contain the contents of current file
        var textview = Text()
        textview = Text(currentFile.readText())
        textview.font = Font.font("Verdana", FontWeight.NORMAL, 14.0)

        // Create a text flow pane and attach text content into the flow pane
        val textFlowPane = TextFlow()

        // Setting the line spacing between the text objects
        textFlowPane.setTextAlignment(TextAlignment.LEFT)
        textFlowPane.setPrefSize(600.0, 300.0)
        textFlowPane.setLineSpacing(5.0)
        textFlowPane.children.add(textview)
        val scrollpane = ScrollPane(textFlowPane)

        stackpane.children.add(scrollpane)
        StackPane.setAlignment(scrollpane,Pos.CENTER)
    }

    // The function will update the status bar to contain the newest path
    fun updateStatus(statusbar: VBox, currentFile: File) {
        statusbar.children.clear()
        statusbar.children.add(Text(currentFile.absolutePath))
    }

    // The function will display the content of current selected file in the fileList to
    //   the stackpane
    fun displayContent(stackpane: StackPane, statusbar: VBox) {
        currentFileName = fileList.items[fileList.selectionModel.selectedIndex]
        val currentFile = File("${dir.absolutePath}\\$currentFileName")
        val fileExtension: String = currentFile.extension
        updateStatus(statusbar, currentFile)
        if (isHidden && currentFile.name.startsWith(".")) {
            // we can not show the content of the file if the file should be hidden
            showNothing(stackpane)
        } else if (currentFile.isDirectory) {
            showNothing(stackpane)
        } else if (fileExtension == "png" || fileExtension == "jpg" || fileExtension == "bmp") {
            displayImage(stackpane, currentFile)
        } else if (fileExtension == "txt" || fileExtension == "md") {
            displayTxt(stackpane, currentFile)
        } else if (!currentFile.canRead()) {
            stackpane.children.clear()
            val message = Text("File cannot be read")
            message.font = Font.font("Verdana", FontWeight.NORMAL, 14.0)
            stackpane.children.add(message)
        } else {
            stackpane.children.clear()
            val message = Text("Unsupported type")
            message.font = Font.font("Verdana", FontWeight.NORMAL, 14.0)
            stackpane.children.add(message)
        }
    }

    // The function will update the fileList and statusbar so that they are
    //   changed to the inner directory if the current file is a directory
    fun goInDirectory(stackpane: StackPane, statusbar: VBox) {
        if (File(dir.absolutePath + "\\$currentFileName").isDirectory) {
            dir = File(dir.absolutePath + "\\$currentFileName")

            // Update fileList to contain the newest directory
            updateFileList(isHidden)
            fileList.selectionModel.selectIndices(0)
            displayContent(stackpane, statusbar)
        }
    }

    // The function will update the fileList and statusbar so that they are
    //   changed to the outer direcotry if the current file is a directory except for root
    fun goOutDirectory(stackpane: StackPane, statusbar: VBox) {
        if ("${System.getProperty("user.dir")}\\test" != dir.absolutePath) {
            dir = File(dir.parent)
            updateStatus(statusbar, dir)

            // Update fileList to contain the newest directory
            updateFileList(isHidden)
            fileList.selectionModel.selectIndices(0)
            displayContent(stackpane, statusbar)
        }
    }

    // The function will retrieve to the home directory
    fun goHomeDirectory(stackpane: StackPane, statusbar: VBox) {
        if (dir.absolutePath != "${System.getProperty("user.dir")}\\test") {
            dir = File("${System.getProperty("user.dir")}\\test")
            updateStatus(statusbar, dir)

            // Update fileList to contain the newest directory
            updateFileList(isHidden)
            fileList.selectionModel.selectIndices(0)
            displayContent(stackpane, statusbar)
        }
    }

    // The function will delete the current file
    fun deleteCurrentFile(stackpane: StackPane, statusbar: VBox) {
        val deleteFile = File(dir.absolutePath + "\\$currentFileName")
        val confirmation = Alert(Alert.AlertType.CONFIRMATION)
        confirmation.title = "Confirmation dialog"
        confirmation.contentText = "Do you wish to delete file $currentFileName?"
        val result = confirmation.showAndWait()

        if (result.isPresent) {
            when (result.get()) {
                ButtonType.OK -> {
                    deleteFile.deleteRecursively()
                    updateStatus(statusbar, dir)

                    // Update fileList to contain the newest directory
                    updateFileList(isHidden)
                    fileList.selectionModel.selectIndices(0)
                    displayContent(stackpane, statusbar)
                }
            }
        }
    }

    // The function will rename the current file depending on the user input
    fun renameCurrentFile(stackpane: StackPane, statusbar: VBox) {
        val renameFile = File(dir.absolutePath + "\\$currentFileName")

        // Ask for text input
        var newFileName: String = ""
        val dialog = TextInputDialog("")
        dialog.title = "Rename file $currentFileName"
        if (renameFile.isDirectory) {
            dialog.headerText = "Enter a new directory name"
        } else {
            dialog.headerText = "Enter a new file name"
        }


        val result = dialog.showAndWait()
        if (result.isPresent) {
            newFileName = result.get()
            val newFile = File(dir.absolutePath + "\\$newFileName")
            if (!renameFile.renameTo(newFile)) {
                error("The file $currentFileName cannot be renamed correctly")
            } else {
                // Update fileList to contain the newest directory
                updateStatus(statusbar, newFile)
                updateFileList(isHidden)

                // We need to add \ to the back of the directory name, so we do file and directory separately
                if (newFile.isDirectory) {
                    newFileName += "\\"
                    fileList.selectionModel.selectIndices(fileList.items.indexOf(newFileName))
                } else {
                    fileList.selectionModel.selectIndices(fileList.items.indexOf(newFileName))
                }
                displayContent(stackpane, statusbar)
            }
        }
    }

    // The function will move the current pointing file or directory to a specified directory that
    //   the user entered or display an error if the destination is invalid
    fun moveFileDirectory(stackpane: StackPane, statusbar: VBox) {
        val moveFile = File(dir.absolutePath + "\\$currentFileName")

        // Ask for text input
        var newFilePath: String = ""
        val dialog = TextInputDialog("")
        dialog.title = "Move file/directory $currentFileName"
        dialog.headerText = "Enter the destination directory"

        val result = dialog.showAndWait()
        if (result.isPresent) {
            newFilePath = result.get()
            val newFile = File(newFilePath + "//${moveFile.name}")
            if (!moveFile.copyRecursively(newFile)) {
                error("The file $currentFileName cannot be moved correctly")
            } else {
                // If the movement is successful, then delete the previous file
                moveFile.deleteRecursively()
                updateStatus(statusbar, dir)
                updateFileList(isHidden)
                fileList.selectionModel.selectIndices(0)
                displayContent(stackpane, statusbar)
            }
        }
    }

    // The function will either reflect the current pointing file by removing another prefix '.' of the
    //   current file name or hid the current pointing file by add the '.' as the prefix of the current
    //   file if there is no depending on the parameter reflect of the function
    fun reflectHiddenFile(stackpane: StackPane, statusbar: VBox, reflect: Boolean) {
        val targetFile = File(dir.absolutePath + "\\$currentFileName")

        if (reflect) {
            // We check that if the current file is hidden
            if (targetFile.name.startsWith(".")) {
                // If the current file is hidden, we reflect the file
                val newFile = File(dir.absolutePath + "\\" + targetFile.name.substring(1))
                targetFile.renameTo(newFile)
                updateStatus(statusbar, newFile)
                updateFileList(isHidden)
                fileList.selectionModel.selectIndices(fileList.items.indexOf(newFile.name))
                displayContent(stackpane, statusbar)
            }
        } else {
            // We check that if the current file is reflected
            if (!targetFile.name.startsWith(".")) {
                val newFile = File(dir.absolutePath + "\\." + targetFile.name)
                targetFile.renameTo(newFile)
                // We display the status of the hidden file depending on whether the file should be shown
                updateFileList(isHidden)
                if (isHidden) {
                    updateStatus(statusbar, dir)
                    fileList.selectionModel.selectIndices(0)
                    displayContent(stackpane, statusbar)
                    stackpane.children.clear()
                } else {
                    updateStatus(statusbar, newFile)
                    fileList.selectionModel.selectIndices(fileList.items.indexOf(newFile.name))
                    displayContent(stackpane, statusbar)
                }
            }
        }
    }
}
