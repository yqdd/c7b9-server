import sys
from piano_transcription_inference import PianoTranscription, sample_rate, load_audio

# Transcriptor
transcriptor = PianoTranscription(device='cuda')    # 'cuda' | 'cpu'

print('加载完成')

while True:
    s = input('file:').split()
    if(s[0] == 'exit'):
        break

    # Load audio
    (audio, _) = load_audio(s[0], sr=sample_rate, mono=True)

    # Transcribe and write out to MIDI file
    transcribed_dict = transcriptor.transcribe(audio, s[1])

    print('转换完成')
