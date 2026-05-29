package client;

import rmi.IStopServer;
import rmi.IStopClient;

import javax.swing.*;
import java.awt.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class StopGameGUI extends JFrame implements IStopClient {
    
    // CONFIGURAÇÃO DE REDE: Altere para o IP da máquina Servidora quando for rodar em rede!
    private static final String IP_SERVIDOR = "192.168.0.33"; 
    private static final int PORTA_SERVIDOR = 1099;

    private IStopServer servidor;
    private String meuNome;
    
    // Componentes Visuais
    private JLabel lblStatus;
    private JLabel lblLetra;
    private JTextField txtNome;
    private JTextField txtCidade;
    private JTextField txtAnimal;
    private JButton btnStop;
    private JButton btnStart;
    private JTextArea txtResultados;
    
    public StopGameGUI(String nome, IStopServer servidor) throws RemoteException {
        this.meuNome = nome;
        this.servidor = servidor;
        
        // Exporta a interface gráfica como objeto remoto para permitir o Callback do Servidor
        UnicastRemoteObject.exportObject(this, 0);
        servidor.registrarJogador(nome, this);
        
        configurarJanela();
    }
    
    private void configurarJanela() {
        setTitle("STOP! - Jogador: " + meuNome);
        setSize(420, 520);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        JPanel painelTopo = new JPanel(new GridLayout(2, 1));
        lblStatus = new JLabel("Conectado! Aguardando início da rodada...", SwingConstants.CENTER);
        lblStatus.setFont(new Font("Arial", Font.PLAIN, 12));
        lblLetra = new JLabel("Letra: ?", SwingConstants.CENTER);
        lblLetra.setFont(new Font("Arial", Font.BOLD, 36));
        lblLetra.setForeground(new Color(43, 108, 176));
        painelTopo.add(lblStatus);
        painelTopo.add(lblLetra);
        add(painelTopo, BorderLayout.NORTH);
        
        JPanel painelCentro = new JPanel(new GridLayout(3, 2, 5, 15));
        painelCentro.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        painelCentro.add(new JLabel("Nome Próprio:"));
        txtNome = new JTextField();
        painelCentro.add(txtNome);
        
        painelCentro.add(new JLabel("Cidade/Estado/País:"));
        txtCidade = new JTextField();
        painelCentro.add(txtCidade);
        
        painelCentro.add(new JLabel("Animal:"));
        txtAnimal = new JTextField();
        painelCentro.add(txtAnimal);
        
        add(painelCentro, BorderLayout.CENTER);
        
        JPanel painelBase = new JPanel(new BorderLayout());
        JPanel painelBotoes = new JPanel();
        btnStart = new JButton("Nova Rodada");
        btnStop = new JButton("STOP!");
        btnStop.setBackground(new Color(229, 62, 62));
        btnStop.setForeground(Color.WHITE);
        btnStop.setFont(new Font("Arial", Font.BOLD, 12));
        
        painelBotoes.add(btnStart);
        painelBotoes.add(btnStop);
        painelBase.add(painelBotoes, BorderLayout.NORTH);
        
        txtResultados = new JTextArea(10, 30);
        txtResultados.setEditable(false);
        txtResultados.setFont(new Font("Monospaced", Font.PLAIN, 12));
        txtResultados.setBorder(BorderFactory.createTitledBorder("Resultados do Servidor"));
        painelBase.add(new JScrollPane(txtResultados), BorderLayout.CENTER);
        
        add(painelBase, BorderLayout.SOUTH);
        
        bloquearCampos();
        
        // Ação do Host para começar
        btnStart.addActionListener(e -> {
            try {
                servidor.iniciarJogo();
            } catch (RemoteException ex) {
                JOptionPane.showMessageDialog(this, "Erro de rede ao iniciar jogo: " + ex.getMessage());
            }
        });
        
        // Ação de Gritar Stop
        btnStop.addActionListener(e -> {
            try {
                servidor.gritarStop(meuNome);
            } catch (RemoteException ex) {
                ex.printStackTrace();
            }
        });
        
        setLocationRelativeTo(null);
        setVisible(true);
    }
    
    private void bloquearCampos() {
        txtNome.setEnabled(false);
        txtCidade.setEnabled(false);
        txtAnimal.setEnabled(false);
        btnStop.setEnabled(false);
    }
    
    private void liberarCampos() {
        txtNome.setEnabled(true);
        txtCidade.setEnabled(true);
        txtAnimal.setEnabled(true);
        btnStop.setEnabled(true);
        
        txtNome.setText("");
        txtCidade.setText("");
        txtAnimal.setText("");
        txtNome.requestFocus();
    }

    // ==========================================
    // MÉTODOS REMOTOS (Invocados pelo Servidor)
    // ==========================================

    @Override
    public void iniciarRodada(char letra) throws RemoteException {
        SwingUtilities.invokeLater(() -> {
            lblStatus.setText("RODADA EM ANDAMENTO! Digite rápido!");
            lblStatus.setForeground(new Color(56, 161, 105));
            lblLetra.setText("Letra: " + letra);
            liberarCampos();
            txtResultados.setText(""); 
        });
    }

    @Override
    public void pararJogo(String quemParou) throws RemoteException {
        SwingUtilities.invokeLater(() -> {
            bloquearCampos();
            lblStatus.setText("FIM DA RODADA! " + quemParou.toUpperCase() + " GRITOU STOP!");
            lblStatus.setForeground(Color.RED);
            
            // Coleta os textos digitados até este momento exato
            List<String> minhasRespostas = new ArrayList<>();
            minhasRespostas.add(txtNome.getText());
            minhasRespostas.add(txtCidade.getText());
            minhasRespostas.add(txtAnimal.getText());
            
            try {
                // Devolve as respostas para a máquina servidora computar
                servidor.enviarRespostas(meuNome, minhasRespostas);
            } catch (RemoteException e) {
                System.err.println("Erro ao enviar dados ao servidor: " + e.getMessage());
            }
        });
    }

    @Override
    public void mostrarResultados(String resultados) throws RemoteException {
        SwingUtilities.invokeLater(() -> {
            txtResultados.setText(resultados);
            lblStatus.setText("Rodada finalizada. Pronto para a próxima.");
            lblStatus.setForeground(Color.BLACK);
        });
    }

    // =========================================================================
    // MAIN DO JOGADOR (Executar em quantas máquinas clientes você quiser)
    // =========================================================================
    public static void main(String[] args) {
        String nome = JOptionPane.showInputDialog(null, "Digite seu nome de jogador:", "Conexão Stop RMI", JOptionPane.QUESTION_MESSAGE);
        
        if (nome == null || nome.trim().isEmpty()) {
            System.exit(0);
        }
        
        try {
            System.out.println("Tentando conectar ao Servidor RMI em: " + IP_SERVIDOR + ":" + PORTA_SERVIDOR);
            
            // Localiza o registro de nomes remoto apontando para o IP configurado
            Registry registry = LocateRegistry.getRegistry(IP_SERVIDOR, PORTA_SERVIDOR);
            
            // Captura a referência do servidor remoto
            IStopServer servidorRemoto = (IStopServer) registry.lookup("StopServer");
            
            // Dispara a interface do jogo passando o servidor localizado na rede
            new StopGameGUI(nome, servidorRemoto);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, 
                "Não foi possível conectar à máquina Servidora.\n" +
                "Verifique se o IP '" + IP_SERVIDOR + "' está correto e se o Servidor está ligado.\n\n" +
                "Erro técnico: " + e.getMessage(), 
                "Erro de Conexão de Rede", 
                JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }
}