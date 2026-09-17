package bo.firmadigital.jacobitus.escritorio.formularios;

import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.platform.unix.X11;
import com.sun.jna.platform.unix.X11.Atom;
import com.sun.jna.platform.unix.X11.AtomByReference;
import com.sun.jna.platform.unix.X11.Display;
import com.sun.jna.platform.unix.X11.Window;
import com.sun.jna.platform.unix.X11.XClientMessageEvent;
import com.sun.jna.platform.unix.X11.XEvent;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.NativeLongByReference;
import com.sun.jna.ptr.PointerByReference;

import bo.firmadigital.jacobitus.utilidades.SistemaOperativoHelper;

class ActivadorVentanaX11 {
    private static final Logger LOG = Logger.getLogger(ActivadorVentanaX11.class.getName());
    private static final int MAX_VENTANAS = 512;
    private static final int MAX_BYTES_NOMBRE = 1024;

    private ActivadorVentanaX11() {
    }

    static boolean activarPorTitulo(String titulo) {
        if (!SistemaOperativoHelper.esUnix()) {
            return false;
        }
        X11 x11 = X11.INSTANCE;
        Display display = x11.XOpenDisplay(null);
        if (display == null) {
            return false;
        }
        try {
            long pid = ProcessHandle.current().pid();
            Window raiz = x11.XDefaultRootWindow(display);
            Atom netClientList = x11.XInternAtom(display, "_NET_CLIENT_LIST", false);
            Atom netWmPid = x11.XInternAtom(display, "_NET_WM_PID", false);
            Atom netWmName = x11.XInternAtom(display, "_NET_WM_NAME", false);
            Atom utf8String = x11.XInternAtom(display, "UTF8_STRING", false);
            Atom netActiveWindow = x11.XInternAtom(display, "_NET_ACTIVE_WINDOW", false);

            Window objetivo = buscarVentana(x11, display, raiz, netClientList, netWmPid, x11.XA_CARDINAL, netWmName,
                    utf8String, pid, titulo);
            if (objetivo == null) {
                LOG.warning("ActivadorVentanaX11: ventana no encontrada");
                return false;
            }
            activar(x11, display, raiz, objetivo, netActiveWindow);
            return true;
        } catch (Exception ex) {
            LOG.warning("ActivadorVentanaX11: " + ex);
            return false;
        } finally {
            x11.XCloseDisplay(display);
        }
    }

    private static Window buscarVentana(X11 x11, Display display, Window raiz, Atom netClientList, Atom netWmPid,
            Atom cardinal, Atom netWmName, Atom utf8String, long pid, String titulo) {
        NativeLongByReference cantidadVentanas = new NativeLongByReference();
        Pointer listaVentanas = leerPropiedad(x11, display, raiz, netClientList, x11.XA_WINDOW, MAX_VENTANAS,
                cantidadVentanas);
        if (listaVentanas == null) {
            return null;
        }
        try {
            int cantidad = (int) cantidadVentanas.getValue().longValue();
            for (int i = 0; i < cantidad; i++) {
                long id = listaVentanas.getNativeLong((long) i * Native.LONG_SIZE).longValue();
                Window ventana = new Window(id);

                if (!coincidePid(x11, display, ventana, netWmPid, cardinal, pid)) {
                    continue;
                }
                if (coincideTitulo(x11, display, ventana, netWmName, utf8String, titulo)) {
                    return ventana;
                }
            }
            return null;
        } finally {
            x11.XFree(listaVentanas);
        }
    }

    private static boolean coincidePid(X11 x11, Display display, Window ventana, Atom netWmPid, Atom cardinal,
            long pid) {
        NativeLongByReference cantidadPid = new NativeLongByReference();
        Pointer pidCrudo = leerPropiedad(x11, display, ventana, netWmPid, cardinal, 1, cantidadPid);
        if (pidCrudo == null) {
            return false;
        }
        try {
            return pidCrudo.getNativeLong(0).longValue() == pid;
        } finally {
            x11.XFree(pidCrudo);
        }
    }

    private static boolean coincideTitulo(X11 x11, Display display, Window ventana, Atom netWmName, Atom utf8String,
            String titulo) {
        NativeLongByReference cantidadNombre = new NativeLongByReference();
        Pointer nombreCrudo = leerPropiedad(x11, display, ventana, netWmName, utf8String, MAX_BYTES_NOMBRE,
                cantidadNombre);
        if (nombreCrudo == null) {
            return false;
        }
        try {
            int cantidadBytes = (int) cantidadNombre.getValue().longValue();
            String nombre = cantidadBytes <= 0 ? null
                    : new String(nombreCrudo.getByteArray(0, cantidadBytes), StandardCharsets.UTF_8);
            return titulo.equals(nombre);
        } finally {
            x11.XFree(nombreCrudo);
        }
    }

    private static Pointer leerPropiedad(X11 x11, Display display, Window ventana, Atom propiedadBuscada,
            Atom tipoEsperado, int maxElementos, NativeLongByReference cantidadDevuelta) {
        AtomByReference tipoDevuelto = new AtomByReference();
        IntByReference formatoDevuelto = new IntByReference();
        NativeLongByReference restanteDevuelto = new NativeLongByReference();
        PointerByReference propiedad = new PointerByReference();
        int resultado = x11.XGetWindowProperty(display, ventana, propiedadBuscada, new NativeLong(0),
                new NativeLong(maxElementos), false, tipoEsperado, tipoDevuelto, formatoDevuelto,
                cantidadDevuelta, restanteDevuelto, propiedad);
        if (resultado != 0) {
            return null;
        }
        return propiedad.getValue();
    }

    private static void activar(X11 x11, Display display, Window raiz, Window objetivo, Atom netActiveWindow) {
        XClientMessageEvent mensaje = new XClientMessageEvent();
        mensaje.type = X11.ClientMessage;
        mensaje.display = display;
        mensaje.window = objetivo;
        mensaje.message_type = netActiveWindow;
        mensaje.format = 32;
        mensaje.data.setType(NativeLong[].class);
        mensaje.data.l[0] = new NativeLong(2);
        mensaje.data.l[1] = new NativeLong(0);
        mensaje.data.l[2] = new NativeLong(0);
        mensaje.data.l[3] = new NativeLong(0);
        mensaje.data.l[4] = new NativeLong(0);
        mensaje.write();

        XEvent evento = new XEvent();
        evento.setType(XClientMessageEvent.class);
        evento.xclient = mensaje;
        evento.write();

        x11.XSendEvent(display, raiz, 0,
                new NativeLong(X11.SubstructureRedirectMask | X11.SubstructureNotifyMask), evento);
        x11.XFlush(display);
    }
}
