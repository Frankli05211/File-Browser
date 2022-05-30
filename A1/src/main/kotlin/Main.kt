import javafx.application.Application
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.BorderPane
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.text.*
import javafx.stage.Stage
import org.w3c.dom.Text
import java.io.File
import java.io.FileInputStream

class Main : Application() {
    // Generate variables to contain file information
    var currentFileName: String = ""
    var fileList = ListView<String>()
    var dir = File("${System.getProperty("user.dir")}\\test")
    var isHidden:Boolean = true

    val BUTTON_MIN_WIDTH = 50.0
    val BUTTON_PREF_WIDTH = 100.0
    val BUTTON_MAX_WIDTH = 200.0
    override fun start(stage: Stage) {
        // initialize the list of file using the root directory
        updateFileList(isHidden)
        fileList.selectionModel.selectIndices(0)

        // Create stack pane at the center of the border pane to contain image
        val stackpane = StackPane()
        stackpane.minHeight = 0.0

        // Display the path of the current file at the bottom left of the border pane
        val statusbar = VBox()
        statusbar.children.add(Text(dir.absolutePath))

        // Select the first item; you don't want an empty selection
        fileList.selectionModel.selectIndices(0)
        fileList.setOnMouseClicked {
            currentFileName = fileList.items[fileList.selectionModel.selectedIndex]
            val currentFile = File("${dir.absolutePath}\\$currentFileName")
            val fileExtension: String = currentFile.extension
            updateStatus(statusbar, currentFile)
            if (currentFile.isDirectory) {
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

            if (it.clickCount == 2) {
                goInDirectory()
            }
        }
        fileList.style = "-fx-background-color: white;"

        // Generate menubar
        val menuBar = MenuBar()
        val fileMenu = Menu("File")
        val viewMenu = Menu("View")
        val menuActions = Menu("Actions")
        val menuOptions = Menu("Options")

        // The menu item for File
        val menuNew = MenuItem("New")
        val menuOpen = MenuItem("Open")
        val menuClose = MenuItem("Close")
        val menuQuit = MenuItem("Quit")
        menuQuit.setOnAction { Platform.exit() }

        // The menu item for View
        val menuReflect = MenuItem("Reflect files")
        val menuHide = MenuItem("Hidden files")

        // The menu item for Actions
        val menuRename = MenuItem("Rename")
        val menuMove = MenuItem("Move")
        val menuDelete = MenuItem("Delete")

        // The menu item for Options
        val menuShow = MenuItem("Show Hidden Files")
        menuShow.setOnAction {
            isHidden = !isHidden
            updateFileList(isHidden)
        }

        // Add menu item to their corresponding menus
        fileMenu.items.addAll(menuNew, menuOpen, menuClose, menuQuit)
        viewMenu.items.addAll(menuReflect, menuHide)
        menuActions.items.addAll(menuRename, menuMove, menuDelete)
        menuOptions.items.addAll(menuShow)
        menuBar.menus.addAll(fileMenu, viewMenu, menuActions, menuOptions)

        // Generate the toolbar
        val toolbar = ToolBar()
        val homeButton: Button = StandardButton("HOME")
        homeButton.setOnAction {
            // If the current directory is not the root directory, we go to the root directory
            goHomeDirectory(statusbar)
        }

        val prevButton: Button = StandardButton("Prev")
        prevButton.setOnAction {
            // if the current directory is not the root directory, we go to the parent directory
            goOutDirectory(statusbar)
        }

        val nextButton: Button = StandardButton("Next")
        nextButton.setOnAction {
            // if the current file is a directory, then we navigate into the current directory, if not,
            //   we do nothing
            goInDirectory()
        }

        val deleteButton: Button = StandardButton("Delete")
        deleteButton.setOnAction {
            deleteCurrentFile(statusbar)
        }

        val renameButton: Button = StandardButton("Rename")
        renameButton.setOnAction {
            renameCurrentFile(statusbar)
        }

        toolbar.items.addAll(homeButton, prevButton, nextButton, deleteButton, renameButton)
        toolbar.padding = Insets(10.0)

        // stack menu and toolbar in the top region
        val vbox = VBox(menuBar, toolbar)

//        imageView = ImageView(Image("Bailey.png"))
//        imageView.fitHeightProperty().bind(stackpane.heightProperty())
//        imageView.fitWidthProperty().bind(stackpane.widthProperty())
//        imageView.isPreserveRatio = true
//        stackpane.children.add(imageView)
//        StackPane.setAlignment(imageView,Pos.CENTER)
//        menuOpen.setOnAction {
//            stackpane.children.remove(imageView)
//            val dir = File("D:\\Spring 2022\\CS 349\\cs349-ass\\a1\\A1\\src\\main\\resources")
//            val stream = FileInputStream("${dir.absolutePath}\\img.png")
//            imageView = ImageView(Image(stream))
//            imageView.fitHeightProperty().bind(stackpane.heightProperty())
//            imageView.fitWidthProperty().bind(stackpane.widthProperty())
//            imageView.isPreserveRatio = true
//            stackpane.children.add(imageView)
//            StackPane.setAlignment(imageView,Pos.CENTER)
//        }
//        val image = ImageView(Image("Bailey.png"))
//        image.fitHeight(stage.getWidth())

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
            minWidth = BUTTON_MIN_WIDTH
            prefWidth = BUTTON_PREF_WIDTH
            maxWidth = BUTTON_MAX_WIDTH
        }
    }

    // The function will update the recent file list
    fun updateFileList(isHidden: Boolean) {
        fileList.items.clear()
        dir.listFiles().forEach {
            if (isHidden) {
                if (!it.name.startsWith(".")) {
                    fileList.items.add( it.name )
                }
            } else {
                fileList.items.add( it.name )
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
        imageView.fitHeightProperty().bind(stackpane.heightProperty())
        imageView.fitWidthProperty().bind(stackpane.widthProperty())
        imageView.isPreserveRatio = true
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

    // The function will update the fileList and statusbar so that they are
    //   changed to the inner directory if the current file is a directory
    fun goInDirectory() {
        if (File(dir.absolutePath + "\\$currentFileName").isDirectory) {
            dir = File(dir.absolutePath + "\\$currentFileName")

            // Update fileList to contain the newest directory
            updateFileList(isHidden)
            fileList.selectionModel.selectIndices(0)
        }
    }

    // The function will update the fileList and statusbar so that they are
    //   changed to the outer direcotry if the current file is a directory except for root
    fun goOutDirectory(statusbar: VBox) {
        if ("${System.getProperty("user.dir")}\\test" != dir.absolutePath) {
            dir = File(dir.parent)
            updateStatus(statusbar, dir)

            // Update fileList to contain the newest directory
            updateFileList(isHidden)
            fileList.selectionModel.selectIndices(0)
        }
    }

    // The function will retrieve to the home directory
    fun goHomeDirectory(statusbar: VBox) {
        if (dir.absolutePath != "${System.getProperty("user.dir")}\\test") {
            dir = File("${System.getProperty("user.dir")}\\test")
            updateStatus(statusbar, dir)

            // Update fileList to contain the newest directory
            updateFileList(isHidden)
            fileList.selectionModel.selectIndices(0)
        }
    }

    // The function will delete the current file
    fun deleteCurrentFile(statusbar: VBox) {
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
                }
            }
        }
    }

    // The function will rename the current file depending on the user input
    fun renameCurrentFile(statusbar: VBox) {
        val renameFile = File(dir.absolutePath + "\\$currentFileName")

        // Ask for text input
        var newFileName: String = ""
        val dialog = TextInputDialog("")
        dialog.title = "Rename file $currentFileName"
        dialog.headerText = "Enter a new file name"

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
                fileList.selectionModel.selectIndices(fileList.items.indexOf(newFileName))
            }
        }
    }
}
