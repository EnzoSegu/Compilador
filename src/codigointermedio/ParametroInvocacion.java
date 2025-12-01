package codigointermedio;

public class ParametroInvocacion {
    String nombreDestino;
    PolacaElement valor;

    public ParametroInvocacion(String nombre, PolacaElement valor) {
        this.nombreDestino = nombre;
        this.valor = valor;
    }
    public String getNombreDestino() {
        return nombreDestino;
    }
    public PolacaElement getValor() {
        return valor;
    }
    public void setNombreDestino(String nombreDestino) {
        this.nombreDestino = nombreDestino;
    }
    public void setValor(PolacaElement valor) {
        this.valor = valor;
    }

    
}