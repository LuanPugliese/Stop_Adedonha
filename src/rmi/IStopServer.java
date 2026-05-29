package rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public interface IStopServer extends Remote {
    void registrarJogador(String nome, IStopClient cliente) throws RemoteException;
    void iniciarJogo() throws RemoteException;
    void gritarStop(String nome) throws RemoteException;
    void enviarRespostas(String nome, List<String> respostas) throws RemoteException;
    void enviarVotos(String nomeJogador, Map<String, Boolean> votos) throws RemoteException;
}