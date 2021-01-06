package playground.nidhi.examJan;

import java.util.Scanner;

public class SwapProgram {
    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        System.out.println("Enter the value of a: ");
        int a = sc.nextInt();
        System.out.println("Enter the value of b: ");
        int b= sc.nextInt();
        System.out.println("Value of a: " + a);
        System.out.println("Value of b: "+ b);


        int m= a;
        int n = b;
        a=n;
        b=m;
        System.out.println("New Value of a: " + a);
        System.out.println("New Value of b: "+ b);

    }
}
