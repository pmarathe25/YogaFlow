const fs = require('fs');

function writeWav(filename, frequency) {
    const sampleRate = 44100;
    const duration = 1; // seconds
    const numSamples = sampleRate * duration;
    
    const buffer = Buffer.alloc(44 + numSamples * 2);
    
    // RIFF chunk descriptor
    buffer.write('RIFF', 0);
    buffer.writeUInt32LE(36 + numSamples * 2, 4);
    buffer.write('WAVE', 8);
    
    // fmt sub-chunk
    buffer.write('fmt ', 12);
    buffer.writeUInt32LE(16, 16); // Subchunk1Size (16 for PCM)
    buffer.writeUInt16LE(1, 20); // AudioFormat (1 for PCM)
    buffer.writeUInt16LE(1, 22); // NumChannels (1)
    buffer.writeUInt32LE(sampleRate, 24); // SampleRate
    buffer.writeUInt32LE(sampleRate * 2, 28); // ByteRate
    buffer.writeUInt16LE(2, 32); // BlockAlign
    buffer.writeUInt16LE(16, 34); // BitsPerSample
    
    // data sub-chunk
    buffer.write('data', 36);
    buffer.writeUInt32LE(numSamples * 2, 40);
    
    for (let i = 0; i < numSamples; i++) {
        const t = i / sampleRate;
        const sample = Math.sin(2 * Math.PI * frequency * t) * 32767 * 0.1;
        buffer.writeInt16LE(Math.round(sample), 44 + i * 2);
    }
    
    fs.writeFileSync(filename, buffer);
}

const dir = 'app/src/main/res/raw';
if (!fs.existsSync(dir)){
    fs.mkdirSync(dir, { recursive: true });
}

writeWav(dir + '/track_1_meditation.wav', 440);
writeWav(dir + '/track_2_ocean.wav', 220);
writeWav(dir + '/track_3_ambient.wav', 330);
console.log('Created wav files');
