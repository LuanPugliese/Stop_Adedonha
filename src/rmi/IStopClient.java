package rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IStopClient extends Remote {
    void iniciarRodada(char letra) throws RemoteException;
    void pararJogo(String quemParou) throws RemoteException;
    void mostrarResultados(String resultados) throws RemoteException;
}