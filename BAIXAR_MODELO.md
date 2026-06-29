# 🧠 Como Baixar o Modelo FaceNet (Obrigatório para boa precisão)

## O que é?
O **facenet.tflite** é um modelo de rede neural que gera "impressões digitais" 
do rosto de uma pessoa (128 números). É o que faz o reconhecimento funcionar de verdade.

## Download (~3MB)

### Opção 1 — GitHub (recomendado):
1. Vá em: https://github.com/shubham0204/OnDevice-Face-Recognition-Android
2. Baixe: `app/src/main/assets/facenet.tflite`

### Opção 2 — Link direto:
Baixe de: https://drive.google.com/file/d/13vQyFe_kVdO_cpvRjQupEy643DjXQICr/view

## Onde colocar:
```
EscolaNuklearAndroid/
  app/
    src/
      main/
        assets/           ← CRIE ESTA PASTA se não existir
          facenet.tflite   ← COLOQUE AQUI
```

No Android Studio:
1. Clique com botão direito em `app/src/main`
2. **New → Directory → assets**
3. Arraste o arquivo `facenet.tflite` para dentro

## Sem o modelo:
O app funciona com um fallback baseado em cores/proporções, 
mas a precisão é bem inferior (~60% vs ~98% com FaceNet).

## Com o modelo:
Precisão ~98% em condições normais (boa iluminação, rosto visível).
