package com.labsec.desafio;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.encoders.Base64;

import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

import java.io.FileOutputStream;
import java.io.IOException;

import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;

import java.util.Date;

@SpringBootApplication
@RestController
public class DesafioApplication {
	public static void main(String[] args) {
		SpringApplication.run(DesafioApplication.class, args);
	}

	@CrossOrigin
	@PostMapping(path = "/remover", consumes = "*")
	public void remove(@RequestBody JSONObject json) {
		String lista = (String) json.get("listaDelecao");
		String url = "jdbc:sqlite:LabsecDB.db";

		// DELETE FROM table WHERE id IN (1, 4, 6, 7)
		String sql = "DELETE FROM CERTIFICADOS WHERE INT_ID IN ( " + lista + ")";

		try (
				Connection conn = DriverManager.getConnection(url);
				PreparedStatement pstmt = conn.prepareStatement(sql);
		) {
			pstmt.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@CrossOrigin
	@PostMapping(path = "/consulta", consumes = "*")
	public JSONArray buscaBanco(@RequestBody JSONObject json) {
		String nomeEmissor = (String) json.get("entidade");
		String strDataInicial = (String) json.get("data_hora_inicial");
		String strDataFinal = (String) json.get("data_hora_final");

		if (strDataInicial.length() >= 0 && strDataFinal.length() >0) {
			String[] temp = strDataInicial.split("/");
			strDataInicial = temp[2] + "-" + temp[1] + "-" + temp[0];
			temp = strDataFinal.split("/");
			strDataFinal = temp[2] + "-" + temp[1] + "-" + temp[0];
		}

		String url = "jdbc:sqlite:LabsecDB.db";
		String sql = "SELECT INT_ID, TEXT_TITULAR,DATE_INICIO_VALIDADE, DATE_FIM_VALIDADE, TEXT_NUMERO_SERIE, TEXT_CHAVE_PUBLICA";

		if ((!strDataInicial.isEmpty() && !strDataFinal.isEmpty())){
			sql += ", (julianday(DATE_FIM_VALIDADE) - julianday('" + strDataInicial + "')) as FIMVALIDADE, (julianday(DATE_INICIO_VALIDADE) - julianday('" + strDataFinal + "')) as INICIOVALIDADE  ";
		}
		sql += " FROM CERTIFICADOS";

		if (!nomeEmissor.isEmpty() || (!strDataInicial.isEmpty() && !strDataFinal.isEmpty())){
			sql += " WHERE ";
			if (!nomeEmissor.isEmpty()){
				sql += "TEXT_TITULAR LIKE '%" + nomeEmissor + "%'";
			}
			if (!nomeEmissor.isEmpty() && (!strDataInicial.isEmpty() && !strDataFinal.isEmpty())){
				sql += " AND ";
			}
			if (!strDataInicial.isEmpty() && !strDataFinal.isEmpty()){
				sql += "(FIMVALIDADE <= 0 OR INICIOVALIDADE <= 0 )";
			}
		}
		JSONArray retorno = new JSONArray();

		try (
				Connection conn = DriverManager.getConnection(url);
				PreparedStatement pstmt = conn.prepareStatement(sql)
		) {
			ResultSet rs = pstmt.executeQuery();
			while(rs.next()){
				String[] temp = rs.getString("DATE_INICIO_VALIDADE").split("-");
				String strRetDataInicial = temp[2] + "/" + temp[1] + "/" + temp[0];
				temp = rs.getString("DATE_FIM_VALIDADE").split("-");
				String strRetDataFinal = temp[2] + "/" + temp[1] + "/" + temp[0];

				JSONObject obj = new JSONObject();
					obj.put("index",rs.getInt("INT_ID"));
					obj.put("titular", rs.getString("TEXT_TITULAR"));
					obj.put("dataInicioValidade",strRetDataInicial);
					obj.put("dataFimValidade",strRetDataFinal);
					obj.put("numeroSerie",rs.getString("TEXT_NUMERO_SERIE"));
					obj.put("chavePublica",rs.getString("TEXT_CHAVE_PUBLICA"));
				retorno.add(obj);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return retorno;
	}

	@CrossOrigin
	@PostMapping(path = "/cert", consumes = "*")
	public String geraArmazenaCertificado(@RequestBody JSONObject json) throws CertificateException, NoSuchProviderException, NoSuchAlgorithmException, IOException, OperatorCreationException, KeyStoreException {
		String nomeEmissor = (String) json.get("entidade");
		Date dataInicioValidade = null;
		Date dataFimValidade = null;
		try {
			dataInicioValidade = new SimpleDateFormat("dd/MM/yyyy").parse((String) json.get("data_hora_inicial"));
			dataFimValidade = new SimpleDateFormat("dd/MM/yyyy").parse((String) json.get("data_hora_final"));
		} catch (ParseException e) {
			e.printStackTrace();
		}

		//adiciona bouncy castle como provedor de servicos de seguranca para a api de seguranca do java
		Security.addProvider(new BouncyCastleProvider());
		// Certificate Factory e usado para gerar certificados, certpaths e CRLs
		// x509 e o tipo do certificado, bc e o provedor de seguranca (bouncy castle)
		CertificateFactory fabricaCertificados = CertificateFactory.getInstance("X.509", "BC");

		// cria um gerador para o par de chaves
		KeyPairGenerator geradorParChaves = KeyPairGenerator.getInstance("RSA", fabricaCertificados.getProvider());
		geradorParChaves.initialize(2048);

		// cria um par de chaves para o certificado
		KeyPair parChaves = geradorParChaves.generateKeyPair();
		// define caneta, que cria assinaturas a partir de uma entrada
		// JcaContentSignerBuilder e usado para ~ criar um modelo ~ do certificado que sera gerado
		// este e criado usando o algoritmo sha256 com rsa
		// o provedor de servicos e o mesmo da fabrica de certificados e a caneta inicializa com a chave privada definida acima
		ContentSigner caneta = new JcaContentSignerBuilder("SHA256withRSA").setProvider(fabricaCertificados.getProvider()).build(parChaves.getPrivate());

		// define quem vai assinar
		X500Name entidade = new X500Name("CN=" + nomeEmissor);

		// define o numero de serie
		SecureRandom random = new SecureRandom();
		//      gera um numero aleatorio com 20 bytes de tamanho sempre positivo (RFC 5280 4.1.2.2) que vai ser o numero de serie
		byte[] bytes = new byte[20];
		random.nextBytes(bytes);
		BigInteger numeroSerie = new BigInteger(bytes).abs();

		// cria o construtor para um certificado x509 v3
		JcaX509v3CertificateBuilder construtorCertificado = new JcaX509v3CertificateBuilder(entidade, numeroSerie, dataInicioValidade, dataFimValidade, entidade, parChaves.getPublic());

		// cria um objeto que guarda a id para a propriedade de certificado de autoridade e um que guarda o valor true   (RFC 5280 4.2.1.9)
		ASN1ObjectIdentifier certificadoAutoridadeOID = new ASN1ObjectIdentifier(Extension.basicConstraints.getId());
		ASN1Encodable certificadoAutoridadeValor = new BasicConstraints(true);
		// define esse valor a essa id, com a identificacao de extensao critica
		construtorCertificado.addExtension(certificadoAutoridadeOID, true, certificadoAutoridadeValor);

		// JcaX509ExtensionUtils e usada para gerar a Subject Key IDentifier, identificando que o certificado possui dada chave publica (RFC 5280 4.2.1.2)
		JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
		// cria um objeto que guarda a id para a propriedade de SKID
		ASN1ObjectIdentifier SKID_OID = new ASN1ObjectIdentifier(Extension.subjectKeyIdentifier.getId());
		// define esse valor a essa id, com a identificacao de extensao nao critica
		construtorCertificado.addExtension(SKID_OID, false, extUtils.createSubjectKeyIdentifier(parChaves.getPublic()));

		// constroi o Holder para a estrutura do certificado conforme configurado
		X509CertificateHolder holderCertificado = construtorCertificado.build(caneta);

		// cria o certificado pelo construtor base, com provedor de seguranca padrao, muda para o provedor da fabrica de certificados e por fim converte de CertificateHolder para Certificate
		X509Certificate certificado = new JcaX509CertificateConverter().setProvider(fabricaCertificados.getProvider()).getCertificate(holderCertificado);

		// guarda o certificado em base64 no .cer, adionando cabecalho e rodape
		FileOutputStream arquivoCertificado;
		try {
			arquivoCertificado = new FileOutputStream("Certificados/certificado" + numeroSerie.toString().substring(0,5) + ".cer");
			arquivoCertificado.write("-----BEGIN CERTIFICATE-----\n".getBytes());
			arquivoCertificado.write(Base64.encode(certificado.getEncoded()));
			arquivoCertificado.write("\n-----END CERTIFICATE-----".getBytes());
			arquivoCertificado.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// exporta o par de chaves para o arquivo .pfx
		// retorna um objeto de armazenamento para a chave privada
		KeyStore chaveiro = KeyStore.getInstance("PKCS12");
		// abre espaco no objeto
		chaveiro.load(null, null);

		// converte o certificado para um array de certificados tipo java e armazena-o num arquivo ".pfx"
		Certificate[] cert = new Certificate[]{certificado};
		chaveiro.setKeyEntry(nomeEmissor + "_certificado", parChaves.getPrivate(), "1234".toCharArray(), cert);
		FileOutputStream arquivoCertificadoChavePrivada;
		try {
			arquivoCertificadoChavePrivada = new FileOutputStream("Certificados/certificado" + numeroSerie.toString().substring(0,5) + ".pfx");
			chaveiro.store(arquivoCertificadoChavePrivada, "1234".toCharArray());
			arquivoCertificadoChavePrivada.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// prepara a chave publica para ser guardada no banco em um formato levemente mais legivel
		SubjectPublicKeyInfo subPubKeyInfo = SubjectPublicKeyInfo.getInstance(parChaves.getPublic().getEncoded());

		insereBanco(nomeEmissor, numeroSerie, dataInicioValidade, dataFimValidade, subPubKeyInfo.getPublicKeyData().toString().substring(7));

		return certificado.getPublicKey().toString();
	}

	//CREATE TABLE CERTIFICADOS(
	// 	INT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
	// 	TEXT_TITULAR TEXT NOT NULL,
	// 	TEXT_NUMERO_SERIE TEXT NOT NULL,
	// 	DATE_INICIO_VALIDADE DATE NOT NULL,
	// 	DATE_FIM_VALIDADE DATE NOT NULL,
	// 	TEXT_CHAVE_PUBLICA TEXT NOT NULL
	// 	);
	public void insereBanco(String titular, BigInteger numeroSerie, Date inicioValidade, Date fimValidade, String stringChavePublica){
		String url = "jdbc:sqlite:LabsecDB.db";

		String strInicioValidade = new SimpleDateFormat("yyyy-MM-dd").format(inicioValidade);
		String strFimValidade =  new SimpleDateFormat("yyyy-MM-dd").format(fimValidade);

		String sql = "INSERT INTO CERTIFICADOS (TEXT_TITULAR, TEXT_NUMERO_SERIE, DATE_INICIO_VALIDADE, DATE_FIM_VALIDADE, TEXT_CHAVE_PUBLICA) VALUES (?,?, date(?) ,date(?) ,?)";
		try (
			Connection conn = DriverManager.getConnection(url);
			PreparedStatement pstmt = conn.prepareStatement(sql)
		) {
			pstmt.setString(1, titular);
			pstmt.setString(2, numeroSerie.toString());
			pstmt.setString(3, strInicioValidade);
			pstmt.setString(4, strFimValidade);
			pstmt.setString(5, stringChavePublica);

			pstmt.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}





}
