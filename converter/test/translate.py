import io
import os

from piano_transcription_inference import PianoTranscription, sample_rate, load_audio


transcriptor = PianoTranscription(device='cuda')  # 'cuda' | 'cpu'

files = os.listdir("./audio")
for file in files:
    file_path = os.path.join("./audio", file)

    (audio, _) = load_audio(file_path, sr=sample_rate)
    new_file = file.replace(".mp3", ".mid").replace(".m4a", ".mid")
    transcriptor.transcribe(audio, new_file)
