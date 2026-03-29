import mido

from midi import convert_midi_to_str, FilterConfig, VocabConfig

cfg = VocabConfig.from_json("D:/c7b9-converter/tokenizer/vocab_config.json")
filter_cfg = FilterConfig.from_json("D:/c7b9-converter/tokenizer/filter_config.json")
mid = mido.MidiFile("audios-104.mid")
