package com.lsit.dfds.utils;

import java.util.List;
import java.util.Vector;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import lombok.RequiredArgsConstructor;

@Component("bankSftpClient") // give it a unique name
@RequiredArgsConstructor
public class H2HSftpClient {

	@Value("${h2h.sftp.host}")
	private String host;
	@Value("${h2h.sftp.port:22}")
	private int port;
	@Value("${h2h.sftp.username}")
	private String user;
	@Value("${h2h.sftp.password}")
	private String pass;
	@Value("${h2h.sftp.in-dir}")
	private String inDir;
	@Value("${h2h.sftp.out-dir}")
	private String outDir;
	@Value("${h2h.sftp.strict-host-key:false}")
	private boolean strictHostKey;

	private ChannelSftp connect() throws JSchException {
		JSch jsch = new JSch();
		java.util.Properties cfg = new java.util.Properties();
		cfg.put("StrictHostKeyChecking", strictHostKey ? "yes" : "no");

		Session session = jsch.getSession(user, host, port);
		session.setPassword(pass);
		session.setConfig(cfg);
		session.connect(30_000);

		Channel channel = session.openChannel("sftp");
		channel.connect(30_000);
		return (ChannelSftp) channel;
	}

	public void upload(String localPath, String remoteFileName) {
		try {
			ChannelSftp sftp = connect();
			try {
				sftp.cd(inDir);
				sftp.put(localPath, remoteFileName);
			} finally {
				Session s = sftp.getSession();
				sftp.disconnect();
				if (s != null)
					s.disconnect();
			}
		} catch (Exception e) {
			throw new RuntimeException("SFTP upload failed: " + e.getMessage(), e);
		}
	}

	public List<String> listOutDir() {
		try {
			ChannelSftp sftp = connect();
			try {
				sftp.cd(outDir);
				@SuppressWarnings("unchecked")
				Vector<ChannelSftp.LsEntry> list = sftp.ls(".");
				return list.stream().map(ChannelSftp.LsEntry::getFilename)
						.filter(n -> !n.equals(".") && !n.equals("..")).toList();
			} finally {
				Session s = sftp.getSession();
				sftp.disconnect();
				if (s != null)
					s.disconnect();
			}
		} catch (Exception e) {
			throw new RuntimeException("SFTP list failed: " + e.getMessage(), e);
		}
	}
}
