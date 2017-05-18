package utils;

import com.sshtools.net.SocketTransport;
import com.sshtools.ssh.*;
import com.sshtools.ssh.components.SshPublicKey;

public class RemoteShellUtils {

    public static boolean connect(String hostname, int port, String username, String password) {
        boolean result = false;
        try {
            SshConnector con = SshConnector.createInstance();
            con.getContext().setHostKeyVerification(new HostKeyVerification() {
                @Override
                public boolean verifyHost(String s, SshPublicKey sshPublicKey) throws SshException {
                    return true;
                }
            });
            SocketTransport transport = new SocketTransport(hostname, port);
            SshClient ssh = con.connect(transport, username, true);
            PasswordAuthentication pwd = new PasswordAuthentication();
            pwd.setPassword(password);
            result = (ssh.authenticate(pwd) == SshAuthentication.COMPLETE);
            ssh.disconnect();
        } catch (SshException e) {
            e.printStackTrace();
            return connect(hostname, port, username, password);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static void main(String args[]) {
        System.out.println(connect("127.0.0.1", 22, "root", "nideshengria"));
    }
}  