package bo.firmadigital.jacobitus.pesaje;

import javafx.application.Application;

import bo.firmadigital.jacobitus.pesaje.ui.Aplicacion;

/**
 * Punto de entrada del Sistema de Pesaje.
 *
 * La aplicacion (shell, login y navegacion) vive en
 * {@link bo.firmadigital.jacobitus.pesaje.ui.Aplicacion}; aqui solo se
 * arranca JavaFX.
 */
public class Main {

    public static void main(String[] args) {
        Application.launch(Aplicacion.class, args);
    }
}
