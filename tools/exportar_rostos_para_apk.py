"""
═══════════════════════════════════════════════════════════
  Escola Nuklear — Exportar Rostos para o APK
═══════════════════════════════════════════════════════════

Rode na pasta raiz do projeto original (onde tem cadastros.json).

USO:
    python exportar_rostos_para_apk.py

O script detecta automaticamente a estrutura das pastas.
"""

import os
import sys
import json
import glob

# ═══════════════════════════════════════════════════
# Tentar importar o que tiver disponível
# ═══════════════════════════════════════════════════
try:
    import numpy as np
    HAS_NP = True
except ImportError:
    HAS_NP = False
    print("⚠️  numpy não instalado. pip install numpy")

try:
    from PIL import Image
    HAS_PIL = True
except ImportError:
    HAS_PIL = False
    print("⚠️  Pillow não instalado. pip install pillow")

CADASTROS_JSON = "cadastros.json"
SAIDA = "rostos_para_apk.json"


def main():
    print()
    print("☢ Escola Nuklear — Exportar Rostos para APK")
    print("=" * 55)
    print()

    # ─── 1. Verificar que estamos na pasta certa ───
    if not os.path.exists(CADASTROS_JSON):
        print(f"❌ '{CADASTROS_JSON}' não encontrado!")
        print(f"   Rode este script na pasta raiz do projeto original")
        print(f"   (onde fica app.py, cadastros.json, etc)")
        sys.exit(1)

    # ─── 2. Carregar cadastros ───
    with open(CADASTROS_JSON, "r", encoding="utf-8") as f:
        cadastros = json.load(f)

    alunos = {k: v for k, v in cadastros.items() if v.get("tipo") == "aluno"}
    print(f"📋 Cadastros: {len(cadastros)} total, {len(alunos)} alunos")
    print()

    # ─── 3. Descobrir onde estão as fotos ───
    # Listar todas as pastas candidatas
    pastas_candidatas = ["clusters", "rostos_extraidos", "fotos_treino", "alunos"]
    pasta_rostos = None

    for p in pastas_candidatas:
        if os.path.isdir(p):
            subpastas = [d for d in os.listdir(p) if os.path.isdir(os.path.join(p, d))]
            if subpastas:
                print(f"📁 Encontrada: {p}/ ({len(subpastas)} subpastas)")
                if pasta_rostos is None:
                    pasta_rostos = p

    if pasta_rostos is None:
        print("❌ Nenhuma pasta de rostos encontrada!")
        print("   Esperava encontrar: clusters/, rostos_extraidos/, fotos_treino/ ou alunos/")
        sys.exit(1)

    print(f"\n🔍 Usando pasta: {pasta_rostos}/")

    # ─── 4. Mapear grupos → pastas reais ───
    subpastas_reais = {}
    for item in os.listdir(pasta_rostos):
        caminho = os.path.join(pasta_rostos, item)
        if os.path.isdir(caminho):
            subpastas_reais[item] = caminho

    print(f"   Subpastas encontradas: {len(subpastas_reais)}")
    print()

    # ─── 5. Para cada aluno, encontrar sua pasta ───
    resultado = {
        "version": "1.0",
        "embedding_dim": 128,
        "alunos": []
    }

    total_ok = 0
    total_sem_pasta = 0
    total_sem_foto = 0

    for grupo_key, info in alunos.items():
        nome = info.get("nome", grupo_key)

        # Tentar encontrar a pasta do grupo
        # Testar várias variações do nome
        pasta_encontrada = None
        tentativas = [
            grupo_key,                          # "grupo_000"
            grupo_key.replace(".jpg", ""),       # sem extensão
            grupo_key.replace(".png", ""),
        ]

        for t in tentativas:
            if t in subpastas_reais:
                pasta_encontrada = subpastas_reais[t]
                break

        if pasta_encontrada is None:
            print(f"  ⚠️  {nome}: pasta '{grupo_key}' não encontrada em {pasta_rostos}/")
            total_sem_pasta += 1
            continue

        # Pegar todas as fotos da pasta
        fotos = []
        for ext in ['*.jpg', '*.jpeg', '*.png', '*.bmp']:
            fotos.extend(glob.glob(os.path.join(pasta_encontrada, ext)))
            fotos.extend(glob.glob(os.path.join(pasta_encontrada, ext.upper())))

        if not fotos:
            print(f"  ⚠️  {nome}: pasta encontrada mas sem fotos")
            total_sem_foto += 1
            continue

        # Gerar embeddings
        embeddings = []
        for foto_path in fotos:
            emb = gerar_embedding(foto_path)
            if emb is not None:
                embeddings.append(emb)

        if not embeddings:
            print(f"  ⚠️  {nome}: não foi possível processar nenhuma foto")
            continue

        # Média dos embeddings (mais robusto)
        if HAS_NP:
            media = np.mean(embeddings, axis=0)
            norm = np.linalg.norm(media)
            if norm > 0:
                media = media / norm
            embedding_final = media.tolist()
        else:
            # Média manual
            dim = len(embeddings[0])
            media = [0.0] * dim
            for emb in embeddings:
                for i in range(dim):
                    media[i] += emb[i]
            for i in range(dim):
                media[i] /= len(embeddings)
            # Normalizar
            norm = sum(x * x for x in media) ** 0.5
            if norm > 0:
                media = [x / norm for x in media]
            embedding_final = media

        resultado["alunos"].append({
            "nome": nome,
            "grupo": grupo_key,
            "embedding": embedding_final,
            "num_fotos": len(embeddings)
        })

        total_ok += 1
        print(f"  ✅ {nome} — {len(embeddings)} foto(s)")

    # ─── 6. Salvar ───
    with open(SAIDA, "w", encoding="utf-8") as f:
        json.dump(resultado, f, indent=2, ensure_ascii=False)

    print()
    print("=" * 55)
    print(f"✅ EXPORTADOS: {total_ok} alunos")
    if total_sem_pasta > 0:
        print(f"⚠️  SEM PASTA: {total_sem_pasta}")
    if total_sem_foto > 0:
        print(f"⚠️  SEM FOTOS: {total_sem_foto}")
    print(f"📄 Arquivo: {SAIDA}")
    print()
    print("PRÓXIMOS PASSOS:")
    print(f"  1. Copie '{SAIDA}' para o celular")
    print(f"  2. No app → ⚙️ Configurações")
    print(f"  3. Primeiro: 👥 Importar alunos.csv")
    print(f"  4. Depois:  🧠 Importar rostos_para_apk.json")
    print()

    if total_sem_pasta > 0:
        print("─── DEBUG: Primeiras subpastas encontradas ───")
        for i, (k, v) in enumerate(subpastas_reais.items()):
            if i >= 10:
                print(f"  ... e mais {len(subpastas_reais) - 10}")
                break
            print(f"  📁 {k}")
        print()
        print("─── DEBUG: Primeiros grupos no cadastros.json ───")
        for i, (k, v) in enumerate(alunos.items()):
            if i >= 10:
                print(f"  ... e mais {len(alunos) - 10}")
                break
            nome = v.get('nome', '?')
            print(f"  📋 '{k}' → {nome}")
        print()
        print("Se os nomes não batem, ajuste o cadastros.json ou renomeie as pastas.")


def gerar_embedding(caminho_foto):
    """Gera embedding de 128 dimensões a partir de uma foto."""
    if not HAS_PIL:
        return None

    try:
        img = Image.open(caminho_foto).convert("RGB").resize((64, 64))
        pixels = list(img.getdata())  # Lista de tuplas (R, G, B)
        w, h = 64, 64

        embedding = [0.0] * 128
        idx = 0

        # Parte 1: Média de cor por grid 4x4 (48 valores)
        block = 16
        for gy in range(4):
            for gx in range(4):
                sum_r, sum_g, sum_b = 0.0, 0.0, 0.0
                count = 0
                for y in range(gy * block, (gy + 1) * block):
                    for x in range(gx * block, (gx + 1) * block):
                        r, g, b = pixels[y * w + x]
                        sum_r += r
                        sum_g += g
                        sum_b += b
                        count += 1
                if idx < 128:
                    embedding[idx] = sum_r / count / 255.0; idx += 1
                if idx < 128:
                    embedding[idx] = sum_g / count / 255.0; idx += 1
                if idx < 128:
                    embedding[idx] = sum_b / count / 255.0; idx += 1

        # Parte 2: Luminância por grid 8x8 (64 valores)
        block2 = 8
        for gy in range(8):
            for gx in range(8):
                if idx >= 128:
                    break
                sum_lum = 0.0
                count = 0
                for y in range(gy * block2, min((gy + 1) * block2, h)):
                    for x in range(gx * block2, min((gx + 1) * block2, w)):
                        r, g, b = pixels[y * w + x]
                        sum_lum += 0.299 * r + 0.587 * g + 0.114 * b
                        count += 1
                if count > 0 and idx < 128:
                    embedding[idx] = sum_lum / count / 255.0
                    idx += 1

        # Parte 3: Gradientes horizontais (preencher o resto)
        for y in range(0, h - 1, 4):
            for x in range(0, w - 1, 4):
                if idx >= 128:
                    break
                r1, g1, b1 = pixels[y * w + x]
                r2, g2, b2 = pixels[y * w + x + 1]
                lum1 = 0.299 * r1 + 0.587 * g1 + 0.114 * b1
                lum2 = 0.299 * r2 + 0.587 * g2 + 0.114 * b2
                embedding[idx] = (lum2 - lum1) / 255.0
                idx += 1

        # Normalizar L2
        norm = sum(x * x for x in embedding) ** 0.5
        if norm > 0:
            embedding = [x / norm for x in embedding]

        return embedding

    except Exception as e:
        return None


if __name__ == "__main__":
    main()
