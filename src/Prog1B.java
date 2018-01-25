import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/*
 *  Name:               Mark Hunter
 *  Course:             CS460
 *  Assignment:         Prog1B
 *  Instructor:         Dr. McCann
 *  Due Date:           1/25/2018 before class (@12:30)
 *  Description:        There are two stages. The first stage analyzes the bianry file and finds the first 5, middle 4/5,
 *                      and last 5 elements and prints them out. If there are less than five elements, it prints out all
 *                      of them in one block. The second stage searches for a date in the binary file used exponential
 *                      binary search and prints out all dates that match the user-inputted date.
 *  Missing features:   The search stage is prettttty slow
 *  Known Bugs:         Search will probably take a year if the bianry file has more than 100 records
 */
public class Prog1B{

    // Prog1B Driver
    public static void main(String[] args){

        Prog1B prog1B = new Prog1B();

        File fileRef;                           // used to create the file
        RandomAccessFile dataStream = null;     // specializes the file I/O
        DataRecord rec = new DataRecord();      // the object to write/read
        long numberOfRecords = 0;               // loop counter for reading file
        long fileLength = 0;                    // length of the input file
        int recordLength;                       // length of each record
        String narr1Format;                     // holds the string format for the narr1 column

        // save the lengths of each String column
        int narr1Length;
        int narr2Length;
        int diagOtherLength;
        int raceOtherLength;
        int stratumLength;

        // Format for the date field of the records
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");

        // Holds all of the records when the file is read
        List<DataRecord> records = new ArrayList<>();

        fileRef = new File(args[0]);

        // Initialize the RAF and set the pointer to the first file length at the end
        // of the file.
        try {
            dataStream = new RandomAccessFile(fileRef,"rw");
            fileLength = dataStream.length();
            dataStream.seek(fileLength - 20);
        } catch (IOException e) {
            System.out.println("I/O ERROR: Something went wrong when setting the file pointer");
            System.exit(-1);
        }

        // Get the lengths of the longest String values in each column
        rec.fetchLengths(dataStream);
        narr1Length = rec.getNarr1Len();
        narr2Length = rec.getNarr2Len();
        diagOtherLength = rec.getDiagOtherLen();
        raceOtherLength = rec.getRaceOtherLen();
        stratumLength = rec.getStratumLen();

        // Set the pointer to the beginning of the file so all of the records can be processed
        // from the binary file
        try {
            dataStream.seek(0);
        } catch (IOException e) {
            System.out.println("I/O ERROR: Unable to seek");
        }

        // Set the record length. 66 bytes plus the length of each string in the record
        recordLength = 66 + rec.getStratumLen() + rec.getRaceOtherLen() +
                rec.getDiagOtherLen() + rec.getNarr1Len() + rec.getNarr2Len();

        // Calculate the number of records in the file
        numberOfRecords = (fileLength - 20) / recordLength;

        // Read all of the records and store them in the ArrayList
        while (numberOfRecords > 0) {
            // Create new object
            rec = new DataRecord();

            // Set the lengths for the new object
            rec.setNarr1Len(narr1Length);
            rec.setNarr2Len(narr2Length);
            rec.setDiagOtherLen(diagOtherLength);
            rec.setRaceOtherLen(raceOtherLength);
            rec.setStratumLen(stratumLength);

            // Fetch the values for the new object and add it to the list
            rec.fetchObject(dataStream);
            records.add(rec);

            // Decrement the number of records left to read
            numberOfRecords--;
        }

        // print the first 5, middle 4/5, and last 5 elements
        narr1Format = "%-" + narr1Length + "s\n";
        prog1B.printFirstMiddleLast(records, narr1Format, sdf);

        System.out.println("\nTotal number of records: " + records.size());

        // Finally, move on to the search phase
        prog1B.searchForTrmtDate(narr1Format, sdf, records);
    }

    /*
     * Search driver for the exponential binary search and regular binary search.
     *
     * format - holds the format string for the narr1 variable
     * sdf - date format
     * records - list of records
     */
    private void searchForTrmtDate(String format, SimpleDateFormat sdf, List<DataRecord> records) {
        Scanner keyboard = new Scanner(System.in);
        String line;
        Date search;

        while(true) {
            System.out.println("\nPlease enter a treatment date (in the format MM/DD/YYYY):");
            line = keyboard.nextLine();

            try {
                search = sdf.parse(line);
                expBinarySearch(format, search, records, sdf);
            } catch(ParseException e) {
                System.out.println("Error! That input is not in the format MM/DD/YYYY");
            }
        }
    }

    /*
     * Exponential binary search function
     *
     * format - string format for narr1 variable
     * search - date to search for
     * records - list of all of the records
     * sdf - date format
     */
    private void expBinarySearch(String format, Date search, List<DataRecord> records, SimpleDateFormat sdf) {
        double exp;
        double prevExp = 0;
        List<DataRecord> resultList = new ArrayList<>();

        for (int i = 0; i < records.size(); i++){
            if(i > 0)
                prevExp = 2 * (Math.pow(2, i - 1) - 1);
            exp = 2 * (Math.pow(2, i) - 1);
            if (exp >= records.size() || records.get((int) exp).getDate().compareTo(search) > 0) {
                resultList = binarySearch(exp, prevExp, search, records);
                break;
            } else if (records.get((int) exp).getDate().compareTo(search) == 0) {
                resultList.add(records.get((int) exp));
                for (int j = (int) exp + 1; j < records.size(); j++) {
                    if(records.get(j).getDate().compareTo(search) > 0)
                        break;
                    if(records.get(j).getDate().compareTo(search) == 0)
                        resultList.add(records.get(j));
                }

                for (int j = (int) exp - 1; j > 0; j--){
                    if(records.get(j).getDate().compareTo(search) < 0)
                        break;
                    if(records.get(j).getDate().compareTo(search) == 0)
                        resultList.add(records.get(j));
                }
                break;
            }
        }

        if(resultList == null)
            return;

        printValuesAndLabels(format, resultList, sdf, 0, resultList.size());
    }

    /*
     * Binary search
     *
     * exp - the last i value in the exponential binary search function
     * prevExp - the second to last i value
     * search - date to search for
     * records - list of DataRecords
     */
    private List<DataRecord> binarySearch(double exp, double prevExp, Date search, List<DataRecord> records) {
        List<DataRecord> result = null;
        int left = (int) prevExp + 1;
        int right = Math.min(records.size(), (int) exp);
        int mid;

        while (left <= right) {
            mid = (left + right) / 2;

            if (search.compareTo(records.get(mid).getDate()) < 0) {
                right = mid - 1;
            } else if (search.compareTo(records.get(mid).getDate()) > 0) {
                left = mid + 1;
            } else if (search.compareTo(records.get(mid).getDate()) == 0) {
                result = new ArrayList<>();
                result.add(records.get(mid));
                for (int i = mid + 1; i < records.size(); i++) {
                    if (records.get(i).getDate().compareTo(search) > 0)
                        break;
                    if (records.get(i).getDate().compareTo(search) == 0)
                        result.add(records.get(i));
                }

                for (int i = mid - 1; i > 0; i--) {
                    if (records.get(i).getDate().compareTo(search) > 0)
                        break;
                    if (records.get(i).getDate().compareTo(search) == 0)
                        result.add(records.get(i));
                }
            }
        }

        if(result == null){
            System.out.println("There are no matching trmt dates in the database");
            return null;
        }

        return result;
    }

    /*
     *  Prints the column labels
     *
     *  format - holds the format string for the narr1 variable since the length is not set
     */
    public void printLabels(String format){
        System.out.format("%-10s", "Case #");
        System.out.format("%-11s", "trmt date");
        System.out.format(format, "narr1");
    }

    /*
     *  Prints the values and the labels out to trim down the lines used.
     *
     *  format - holds the format string for the narr1 variable
     *  records - all of the DataRecords
     *  sdf - format for the Date object to be parsed to a string
     *  startIndex - the index at which the loop starts
     *  max - number where the loop ends
     */
    public void printValuesAndLabels(String format, List<DataRecord> records, SimpleDateFormat sdf, int startIndex, int max){
        printLabels(format);

        for(int i = startIndex; i < max; i++){
            System.out.format("%-10s", records.get(i).getCaseNum());
            System.out.format("%-11s", sdf.format(records.get(i).getDate()));
            System.out.format(format, records.get(i).getNarr1());
        }
    }

    /*
     *  Prints the first 5, middle 4/5, and last 5 records from the binary file if there are that many
     *
     *  records - all records from the binary file
     *  narr1Format - holds the String format for the narr1 field
     *  sdf - format for the date field
     */
    public void printFirstMiddleLast(List<DataRecord> records, String narr1Format, SimpleDateFormat sdf){
        if (records.size() < 5) {
            // Check if the size is exactly 5 because the message changes
            if(records.size() == 5){
                System.out.println("All of the records since there is exactly five:");
            }else {
                System.out.println("All of the records since there are less than five:");
            }

            printValuesAndLabels(narr1Format, records, sdf, 0, records.size());
        }else{
            // Print the first five records
            System.out.println("First 5 records:");
            printValuesAndLabels(narr1Format, records, sdf, 0, 5);
            System.out.println();

            // Check if the number of records is even or odd. Odd == middle 5, even == middle 4
            if (records.size() % 2 == 1) {
                // Print the middle five records
                System.out.println("Middle 5 records:");
                printValuesAndLabels(narr1Format, records, sdf, records.size() / 2 - 2, records.size() / 2 + 3);

            } else {
                // Print the middle four records
                System.out.println("Middle 4 records:");
                printValuesAndLabels(narr1Format, records, sdf, records.size() / 2 - 2, records.size() / 2 + 2);
            }
            System.out.println();

            // Print the last five records
            System.out.println("Last 5 records:");
            printValuesAndLabels(narr1Format, records, sdf, records.size() - 5, records.size());
        }
    }
}