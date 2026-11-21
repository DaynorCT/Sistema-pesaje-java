package bo.firmadigital.jacobitus.pesaje.ui;

import bo.firmadigital.jacobitus.pesaje.auth.UsuarioSesion;
import bo.firmadigital.jacobitus.pesaje.ui.modulos.BalanzaPane;
import bo.firmadigital.jacobitus.pesaje.ui.modulos.BoletaPane;
import bo.firmadigital.jacobitus.pesaje.ui.modulos.CatalogoPane;
import bo.firmadigital.jacobitus.pesaje.ui.modulos.ChoferesPane;
import bo.firmadigital.jacobitus.pesaje.ui.modulos.EmpresasPane;
import bo.firmadigital.jacobitus.pesaje.ui.modulos.InicioPane;
import bo.firmadigital.jacobitus.pesaje.ui.modulos.PesajePane;
import bo.firmadigital.jacobitus.pesaje.ui.modulos.ProveedoresPane;
import bo.firmadigital.jacobitus.pesaje.ui.modulos.ReportesPane;
import bo.firmadigital.jacobitus.pesaje.ui.modulos.UsuariosPane;
import bo.firmadigital.jacobitus.pesaje.ui.modulos.VehiculosPane;

/**
 * Enlaza cada entrada del menú con su panel funcional.
 */
public final class RegistroModulos {

    private RegistroModulos() {
    }

    public static void inicializar(UsuarioSesion usuario) {
        Modulos.registrar(Navegacion.INICIO, InicioPane::new);
        Modulos.registrar(Navegacion.USUARIOS, UsuariosPane::new);
        Modulos.registrar(Navegacion.EMPRESAS, EmpresasPane::new);
        Modulos.registrar(Navegacion.CHOFERES, ChoferesPane::new);
        Modulos.registrar(Navegacion.PROVEEDORES, ProveedoresPane::new);
        Modulos.registrar(Navegacion.PESAJE, () -> new PesajePane(usuario));
        Modulos.registrar(Navegacion.VEHICULOS, VehiculosPane::new);
        Modulos.registrar(Navegacion.PRODUCTOS, () -> new CatalogoPane("productos"));
        Modulos.registrar(Navegacion.ORIGENES, () -> new CatalogoPane("origenes"));
        Modulos.registrar(Navegacion.DESTINOS, () -> new CatalogoPane("destinos"));
        Modulos.registrar(Navegacion.REPORTES, ReportesPane::new);
        Modulos.registrar(Navegacion.BOLETA, BoletaPane::new);
        Modulos.registrar(Navegacion.BALANZA, BalanzaPane::new);
    }
}
