# Chat Application

A Java-based chat application with a graphical user interface that supports private messaging, group chats, and file sharing.

## Features

- User registration and login
- Private messaging between users
- Group chat creation and management
- File sharing in both private and group chats
- Chat history viewing and management
- Real-time online user status updates

## Requirements

- Java 11 or higher
- Maven

## Building the Application

1. Clone the repository
2. Navigate to the project directory
3. Run the following Maven command to build the project:
   ```bash
   mvn clean package
   ```
4. The built JAR file will be in the `target` directory

## Running the Application

1. First, start the server:
   ```bash
   java -jar target/chat-application-1.0-SNAPSHOT-jar-with-dependencies.jar server
   ```

2. Then, start the client(s):
   ```bash
   java -jar target/chat-application-1.0-SNAPSHOT-jar-with-dependencies.jar
   ```

## Using the Application

### Registration and Login
1. Launch the client application
2. Enter your desired username, password, and email
3. Click "Register" to create a new account
4. After registration, you can log in using your username and password

### Private Chat
1. Double-click on a user's name in the "Online Users" list to start a private chat
2. Type your message in the text field and press Enter or click "Send"
3. Use the "Send File" button to share files
4. Click "History" to view past messages
5. Click "Clear" to clear the current chat window

### Group Chat
1. Click "Create Group" to create a new group chat
2. Enter a name for the group
3. Double-click on a group in the "Groups" list to open the group chat window
4. Group chat supports the same features as private chat

### File Sharing
1. Click the "Send File" button in any chat window
2. Select the file you want to share
3. The file will be sent to all participants in the chat

### Chat History
1. Click the "History" button in any chat window to view past messages
2. The history will be displayed in chronological order
3. You can clear the history display using the "Clear" button

## Notes

- The server must be running before any clients can connect
- Multiple clients can connect to the same server
- Users can participate in multiple private and group chats simultaneously
- File sharing is supported for all file types
- Chat history is stored in a SQLite database 