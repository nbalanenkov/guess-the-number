# Guess the Number Game Backend

#### This application implements a backend for a "Guess the Number" game using WebSocket for communication.

### Game Overview
1. The server starts a round and gives players 10 seconds to choose a name and place a bet on a number from 1 to 10 with a specified bet amount
2. After the betting period, the server generates a random number from 1 to 10
3. Players who guess the correct number receive a message with their winnings (9.9 times the bet)
4. All players receive a message with a list of winners after each round
5. New round starts

### Technology stack

- Java 21
- Gradle
- Spring Boot

### Installation and Setup

1. Clone the repository
2. Build the application using Gradle ```gradle build```
3. Run the application ```gradle run```

### How to Play

1. Connect to the **WebSocket** endpoint at localhost:*port*/game
2. Place a bet within the given time frame
3. A bet should be in JSON format, the message should contain name, number and betAmount fields </br>
Example message: ```{ "name": "John", "number": 1, "betAmount": 3 }```
4. Wait for the round outcome to see if you've won or lost

### Testing

- Unit tests cover individual components
- Integration tests ensure components work together correctly

Run the following command to run all tests:
```gradle test```

### Possible further improvements

- Implement a frontend interface for better user interaction
- Add user authentication and persistence for player statistics
- Refactor and improve WebSocket custom communication protocol
