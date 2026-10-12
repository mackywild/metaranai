from pathlib import Path
r = Path(__file__).resolve().parents[1]
b = r / 'app/src/main/java/jp/metaranai/app'
vm = (b / 'MainViewModel.kt').read_text()
client = (b / 'ExternalDiscoveryClient.kt').read_text()
walker = (b / 'RefillPageWalker.kt').read_text()
assert 'onCandidates = { cached ->' in vm and 'onCandidates = { cached, fresh ->' in vm
assert '_genreLensReady.value = hasUnratedBefore' in vm
assert '_genreLensPreparing.value = !hasUnratedBefore' in vm
assert 'shouldContinue = budget::hasTime' in client
assert 'onCandidates(mergeCache(accepted + partial))' in client
assert 'onAccepted(accepted.toList())' in walker
assert 'onPageCompleted(page)' in walker
assert 'connectTimeout = 4_000; readTimeout = 5_000' in client
assert 'MusicBrainzClient(requestTimeoutMs = 4_000)' in client
method = client[client.index('private fun enrichCandidate'):client.index('private fun mergeCache')]
assert method.index('!OverseasPreference.isJapanese') < method.index('val tags = getTopTags')
assert 'knownArtist?.country' in method
print('V0122_DISCOVERY_LATENCY_OK')
