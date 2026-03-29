import io
import os

import mido
from ffmpeg import overwrite_output
from flask import Flask, request, send_file, Response
from piano_transcription_inference import PianoTranscription, sample_rate, load_audio
import numpy as np
import tempfile
import ffmpeg

import midi
from midi import VocabConfig, FilterConfig

app = Flask(__name__)
transcriptor = PianoTranscription(device='cuda')  # 'cuda' | 'cpu'

cfg = VocabConfig.from_json("midi_vocab_config.json")
filter_cfg = FilterConfig.from_json("midi_filter_config.json")
temp_dir = os.path.join(os.getcwd(), "temp")
if not os.path.exists(temp_dir):
    os.mkdir(temp_dir)


@app.route('/m4a_to_midi', methods=['POST'])
def m4a_to_midi():
    # 创建一个临时文件
    with tempfile.NamedTemporaryFile(delete=False, suffix=".m4a", dir=temp_dir) as temp_audio, \
            tempfile.NamedTemporaryFile(delete=False, suffix=".mid", dir=temp_dir) as temp_mid:
        # 将数据保存到临时文件
        temp_audio.write(request.data)
        print(f"数据已保存到临时文件: {temp_audio.name}")

    (audio, _) = load_audio(temp_audio.name, sr=sample_rate)
    transcriptor.transcribe(audio, temp_mid.name)

    with open(temp_mid.name, "rb") as temp_mid:
        mid = temp_mid.read()

    os.remove(temp_audio.name)
    os.remove(temp_mid.name)
    return Response(mid, mimetype='audio/midi')


@app.route('/m4a_to_mp3', methods=['POST'])
def m4a_to_mp3():
    with tempfile.NamedTemporaryFile(delete=False, suffix=".m4a", dir=temp_dir) as temp_m4a:
        temp_m4a.write(request.data)

    with tempfile.NamedTemporaryFile(delete=False, suffix=".mp3", dir=temp_dir) as temp_mp3:
        print(temp_m4a.name, temp_mp3.name)
        ffmpeg.input(temp_m4a.name).output(temp_mp3.name, **{'y': None, 'codec:a': 'libmp3lame', 'qscale:a': 2}).run()
        mp3 = temp_mp3.read()

    os.remove(temp_m4a.name)
    os.remove(temp_mp3.name)
    return Response(mp3, mimetype="audio/mpeg")


@app.route('/mid_to_text', methods=['POST'])
def mid_to_text():
    # 创建一个临时文件
    with tempfile.NamedTemporaryFile(delete=False, suffix=".mid", dir=temp_dir) as temp_mid:
        temp_mid.write(request.data)    

    mid = mido.MidiFile(temp_mid.name)
    text = midi.convert_midi_to_str(cfg, filter_cfg, mid)[0].replace("<start> ", "").replace(" <end>", "")
    os.remove(temp_mid.name)
    return text


@app.route('/text_to_midi', methods=['POST'])
def str_to_mid():
    text = request.get_data(as_text=True)
    mid = midi.convert_str_to_midi(cfg, text)

    # 创建一个临时文件
    with tempfile.NamedTemporaryFile(delete=False, suffix=".mid", dir=temp_dir) as temp_mid:
        mid.save(temp_mid.name)

    mid = open(temp_mid.name, "rb").read()
    os.remove(temp_mid.name)
    return Response(mid, mimetype='audio/midi')


if __name__ == '__main__':
    app.run()
