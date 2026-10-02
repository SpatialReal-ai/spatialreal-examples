import AVFoundation

enum AudioClip {
    /// The mono PCM16 samples of a WAV file in the app bundle, as the SDK expects them.
    /// The clips here are 16 kHz, the rate the session is created with.
    static func pcm16(_ name: String) throws -> Data {
        guard let url = Bundle.main.url(forResource: name, withExtension: "wav") else {
            throw CocoaError(.fileNoSuchFile)
        }
        let file = try AVAudioFile(forReading: url, commonFormat: .pcmFormatInt16, interleaved: true)
        guard let buffer = AVAudioPCMBuffer(pcmFormat: file.processingFormat, frameCapacity: AVAudioFrameCount(file.length)) else {
            throw CocoaError(.fileReadCorruptFile)
        }
        try file.read(into: buffer)
        return Data(bytes: buffer.int16ChannelData![0], count: Int(buffer.frameLength) * MemoryLayout<Int16>.size)
    }
}
