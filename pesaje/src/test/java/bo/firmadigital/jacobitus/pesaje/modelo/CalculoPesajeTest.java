package bo.firmadigital.jacobitus.pesaje.modelo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public class CalculoPesajeTest {

    @Test
    public void elNetoEsLaDiferenciaEntreBrutoYTara() {
        assertEquals(8000, CalculoPesaje.neto(10000, 2000));
        assertEquals(0, CalculoPesaje.neto(5000, 5000));
    }

    @Test
    public void unBrutoNoValidoSeRechaza() {
        try {
            CalculoPesaje.neto(0, 0);
            fail("Se esperaba el rechazo del bruto en cero.");
        } catch (IllegalArgumentException ex) {
            assertEquals("El peso bruto debe ser mayor a cero.", ex.getMessage());
        }
    }

    @Test
    public void unaTaraNegativaSeRechaza() {
        try {
            CalculoPesaje.neto(10000, -1);
            fail("Se esperaba el rechazo de la tara negativa.");
        } catch (IllegalArgumentException ex) {
            assertEquals("La tara no puede ser negativa.", ex.getMessage());
        }
    }

    @Test
    public void unaTaraMayorAlBrutoSeRechaza() {
        try {
            CalculoPesaje.neto(10000, 12000);
            fail("Se esperaba el rechazo de la tara mayor al bruto.");
        } catch (IllegalArgumentException ex) {
            assertEquals("La tara no puede ser mayor al peso bruto.", ex.getMessage());
        }
    }
}
