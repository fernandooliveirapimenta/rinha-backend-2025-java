package com.example;

import com.example.model.Payment;

public class Main {
 public static void main(String[] args) {
       long primary = 130L;
        long fallback = 100L;
        
        // Verifica se a é pelo menos 30% maior que b
        boolean aIs30PercentGreater = primary >= fallback + 0.3 * fallback;
        
        // Verifica se b é pelo menos 30% maior que a
        boolean bIs30PercentGreater = fallback >= primary + 0.3 * primary;
        
        if (aIs30PercentGreater) {

            System.out.println("A é pelo menos 30% maior que o outro.");
        } else if(bIs30PercentGreater) {
            System.out.println("B é pelo menos 30% maior que o outro. Menor valor: ");

        }
        else {
            System.out.println("Nenhum valor é pelo menos 30% maior que o outro");
        }
 }
}
