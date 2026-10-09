/** Resource processing and native preflight use the same authoritative Python plan selector. */
class ClientCiContract {
    static final List SELECTORS = ['ciSuite', 'ciShard', 'focusedSuite', 'affectedSince', 'twoClientSuite', 'encounterSuite',
        'cinnamonSuite', 'defensiveSuite', 'practiceSuite', 'magicSuite', 'fusionSuite', 'runeSuite',
        'mechanicsSuite', 'tourSuite', 'parrySuite', 'signatureSuite', 'tailSuite']

    static Map selection(List entries, Map properties, File selector, File plan,
                         File sourceDescriptor, File processedDescriptor = null) {
        if (entries == null || entries.isEmpty() || !entries.every { it instanceof String && !it.trim().isEmpty() }
                || entries.toSet().size() != entries.size())
            throw new IllegalArgumentException('Full client descriptor must have unique nonempty entrypoints')
        def active = SELECTORS.findAll { properties.containsKey(it) }
        if (active.size() > 1) throw new IllegalArgumentException('Do not combine client suite selectors')
        if (properties.containsKey('ciShard') != properties.containsKey('ciShards'))
            throw new IllegalArgumentException('ciShard and ciShards must be given together')
        if (properties.containsKey('tailFrom') && !properties.containsKey('tailSuite'))
            throw new IllegalArgumentException('tailFrom requires tailSuite')
        if (!active.isEmpty() && !properties.containsKey('ciShard')) return null // Focused/supervised contracts are independent.
        if (['twoClientUnitySuite', 'twoClientRainshieldSuite', 'twoClientCampSuite'].any { properties.containsKey(it) })
            throw new IllegalArgumentException('Companion flags require twoClientSuite')
        def python = System.getProperty('os.name').toLowerCase(Locale.ROOT).startsWith('windows') ? 'python' : 'python3'
        def command = [python, '-I', selector.absolutePath, '--plan', plan.absolutePath,
                       '--source-descriptor', sourceDescriptor.absolutePath]
        if (properties.containsKey('ciShard'))
            command.addAll(['--shard', "${properties.ciShard}/${properties.ciShards}".toString()])
        if (processedDescriptor != null) command.addAll(['--processed-descriptor', processedDescriptor.absolutePath])
        Process process = null
        try {
            process = new ProcessBuilder(command).start()
            def output = new StringBuffer(), errors = new StringBuffer()
            def stdout = process.consumeProcessOutputStream(output)
            def stderr = process.consumeProcessErrorStream(errors)
            process.outputStream.withWriter('UTF-8') { it.write(groovy.json.JsonOutput.toJson(entries)) }
            if (!process.waitFor(15, java.util.concurrent.TimeUnit.SECONDS))
                throw new IllegalArgumentException('Full-client plan selection timed out')
            stdout.join(1000)
            stderr.join(1000)
            if (stdout.isAlive() || stderr.isAlive())
                throw new IllegalArgumentException('Full-client plan selection output did not complete')
            if (process.exitValue() != 0)
                throw new IllegalArgumentException('Full-client plan selection failed: ' + errors.toString().trim())
            return new groovy.json.JsonSlurper().parseText(output.toString()) as Map
        } catch (IOException exc) {
            throw new IllegalArgumentException('Full-client selection requires Python 3 and the checked-in plan selector', exc)
        } finally {
            if (process != null && process.isAlive()) process.destroyForcibly()
            if (process != null) {
                process.inputStream.close()
                process.errorStream.close()
            }
        }
    }

    static Map profile(Map environment, boolean shaders) {
        def allowed = ['WILDERCORD_ANIMATION_GALLERY', 'WILDERCORD_FIREBLOOD_SHOTS',
            'WILDERCORD_MOON_RECEIPT_NONCE', 'WILDERCORD_SHARED_RECEIPT_NONCE', 'WILDERCORD_FULL_CLIENT_LAUNCH_ID']
        def injected = ['JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS', '_JAVA_OPTIONS', 'GRADLE_OPTS', 'JAVA_OPTS']
        def narrowed = environment.keySet().findAll { (it.toString().startsWith('WILDERCORD_') && !(it in allowed)) || it in injected }
        if (!narrowed.isEmpty()) throw new IllegalArgumentException('Complete client execution forbids environment overrides: ' + narrowed.sort().join(', '))
        return [execution: 'complete', animationGallery: environment.WILDERCORD_ANIMATION_GALLERY == '1',
                shaders: shaders, showcase: false, firebloodShots: environment.WILDERCORD_FIREBLOOD_SHOTS == '1']
    }

}
