// Imports the Scanner class so the program can read input from the user (keyboard).
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {

        // "try-with-resources" block:
        // - Creates a TaskRepository object to communicate with the database.
        // - Creates a Scanner object to read user input from the keyboard.
        // - Both will automatically close when the program exits the try block.

        try (TaskRepository repository = new TaskRepository();
             Scanner scanner = new Scanner(System.in)) {

            //Calls the method that creates the tasks table in the database
            repository.createTable();

            // Prints a message to confirm the database is ready to use.
            System.out.println("Database ready.");


            // Boolean flag that controls the CLI loop
            boolean running = true;

            //Main Loop
            while (running = true) {

                // Print the menu
                System.out.println();
                System.out.println("=== TASK LIST ===");
                System.out.println("1) Add task");
                System.out.println("2) List tasks");
                System.out.println("3) Mark done / not done");
                System.out.println("4) Rename task");
                System.out.println("5) View task by id");
                System.out.println("6) Quit");
                System.out.print("Choose: ");

                // Read user input as a string
                String input = scanner.nextLine();

                //The choice variable will cast the input into int
                int choice;
                try {choice = Integer.parseInt(input);}

                //If user typed text instead of number
                catch (NumberFormatException e) {
                    System.out.println("Invalid input. Please enter a number.");

                    //restart loop and show menu again
                    continue;
                }

                switch (choice){

                    //Add task
                    case 1 -> {}

                    //List tasks
                    case 2 -> {}

                    //Mark done/ not done
                    case 3 -> {}

                    //Rename task
                    case 4 -> {}

                    //View task by id
                    case 5 -> {}

                    //Quit
                    case 6 -> {}

                }

            }

        }

        // Catches any unexpected errors in the program.
        catch (Exception e) {

            // Catches any unexpected errors in the program.
            System.err.println("Application error: " + e.getMessage());

            // Prints the full error details for debugging.
            e.printStackTrace();
        }
    }
}