package mr.supnum.bib;

import jakarta.xml.ws.WebFault;

@WebFault(name = "LivreIndisponible", targetNamespace = "http://supnum.mr/bib")
public class LivreIndisponibleException extends Exception {
    private final LivreIndisponibleInfo faultInfo;

    public LivreIndisponibleException(String message, LivreIndisponibleInfo faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    public LivreIndisponibleInfo getFaultInfo() {
        return faultInfo;
    }
}
