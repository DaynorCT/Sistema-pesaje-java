package bo.firmadigital.jacobitus.pesaje.modelo;

/**
 * Reglas de calculo del pesaje.
 *
 * El peso neto es la diferencia entre el bruto y la tara. En el modo
 * integrado el servidor web hace el calculo; en el escritorio se calcula
 * localmente con estas mismas reglas.
 */
public final class CalculoPesaje {

    private CalculoPesaje() {
    }

    /**
     * Calcula el peso neto.
     *
     * @param bruto peso bruto en kilos
     * @param tara tara en kilos
     * @return el peso neto en kilos
     * @throws IllegalArgumentException si el bruto o la tara no son validos
     */
    public static int neto(int bruto, int tara) {
        validar(bruto, tara);
        return bruto - tara;
    }

    /**
     * Recalcula el peso neto del pesaje cuando hay bruto y tara.
     *
     * @param pesaje pesaje a actualizar
     */
    public static void aplicar(Pesaje pesaje) {
        if (pesaje == null || pesaje.getPesoBruto() == null || pesaje.getTara() == null) {
            return;
        }
        pesaje.setPesoNeto(neto(pesaje.getPesoBruto(), pesaje.getTara()));
    }

    /**
     * Valida la pareja bruto/tara de un pesaje.
     *
     * @param bruto peso bruto en kilos
     * @param tara tara en kilos
     * @throws IllegalArgumentException si los valores no son validos
     */
    public static void validar(int bruto, int tara) {
        if (bruto <= 0) {
            throw new IllegalArgumentException("El peso bruto debe ser mayor a cero.");
        }
        if (tara < 0) {
            throw new IllegalArgumentException("La tara no puede ser negativa.");
        }
        if (tara > bruto) {
            throw new IllegalArgumentException("La tara no puede ser mayor al peso bruto.");
        }
    }
}
