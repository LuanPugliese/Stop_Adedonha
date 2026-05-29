package rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface IStopClient extends Remote {
    void iniciarRodada(char letra) throws RemoteException;
    void pararJogo(String quemParou) throws RemoteException;
    void mostrarResultados(String resultados) throws RemoteException;
    void abrirFaseVotacao(Map<String, Map<String, String>> todasRespostas) throws RemoteException;
}