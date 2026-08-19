"""Sonda de conectividad saliente: que puertos y que protocolos deja pasar la red."""
import socket, ssl, sys, concurrent.futures as cf

def tcp(host, port, t=6):
    try:
        s = socket.create_connection((host, port), timeout=t); s.close(); return "TCP-OK"
    except Exception as e:
        return f"TCP-FAIL({type(e).__name__})"

def tls(host, port, t=8, alpn=None, sni=None):
    try:
        ctx = ssl.create_default_context()
        ctx.check_hostname = False
        ctx.verify_mode = ssl.CERT_NONE
        if alpn: ctx.set_alpn_protocols(alpn)
        raw = socket.create_connection((host, port), timeout=t)
        c = ctx.wrap_socket(raw, server_hostname=sni or host)
        r = f"TLS-OK({c.version()}"
        if c.selected_alpn_protocol(): r += f",alpn={c.selected_alpn_protocol()}"
        c.close()
        return r + ")"
    except Exception as e:
        return f"TLS-FAIL({type(e).__name__})"

OBJETIVOS = [
    ("3ae8699b.databases.neo4j.io", 7687, "Aura Bolt"),
    ("3ae8699b.databases.neo4j.io", 443,  "Aura HTTPS"),
    ("demo.neo4jlabs.com",  7687, "Bolt de otro host"),
    ("1.1.1.1",             853,  "DNS-over-TLS"),
    ("imap.gmail.com",      993,  "IMAPS"),
    ("smtp.gmail.com",      465,  "SMTPS"),
    ("smtp.gmail.com",      587,  "SMTP submission"),
    ("github.com",          22,   "SSH"),
    ("ssh.github.com",      443,  "SSH sobre 443"),
    ("www.google.com",      443,  "HTTPS control"),
    ("api.ipify.org",       443,  "HTTPS control 2"),
    ("portquiz.net",        8443, "puerto alto 8443"),
    ("portquiz.net",        9999, "puerto alto 9999"),
    ("portquiz.net",        2222, "puerto alto 2222"),
]

def probar(o):
    host, port, etiqueta = o
    return f"{etiqueta:22} {host}:{port:<5} -> {tcp(host,port):22} {tls(host,port)}"

with cf.ThreadPoolExecutor(max_workers=14) as ex:
    for linea in ex.map(probar, OBJETIVOS):
        print(linea)
