import { config } from 'dotenv-defaults'
import { defineConfig } from 'vite'
import vitePluginOxCss from '@open-xchange/vite-plugin-ox-css'
import vitePluginOxExternals from '@open-xchange/vite-plugin-ox-externals'
import vitePluginOxManifests from '@open-xchange/vite-plugin-ox-manifests'
import gettextPlugin from '@open-xchange/rollup-plugin-po2json'
import vitePluginProxy from '@open-xchange/vite-plugin-proxy'
import rollupPluginCopy from 'rollup-plugin-copy'
import { fileURLToPath, URL } from 'node:url'

config()

let PROXY_URL
try {
  PROXY_URL = new URL(process.env.SERVER)
} catch (e) {
  PROXY_URL = new URL('https://0.0.0.0')
}
const PORT = process.env.PORT
const ENABLE_HMR = process.env.ENABLE_HMR === 'true'
const ENABLE_HTTP_PROXY = process.env.ENABLE_HTTP_PROXY === 'true'
const FRONTEND_URIS = process.env.FRONTEND_URIS || ''
const ENABLE_SECURE_PROXY = process.env.ENABLE_SECURE_PROXY === 'true'
const ENABLE_SECURE_FRONTENDS = process.env.ENABLE_SECURE_FRONTENDS === 'true'

export default defineConfig(({ mode }) => {
  if (mode === 'development') process.env.VITE_DEBUG = 'true'
  return {
    root: './src',
    publicDir: '../public',
    base: mode === 'development' ? PROXY_URL.pathname : './',
    build: {
      minify: 'esbuild',
      target: 'esnext',
      outDir: '../dist',
      assetsDir: './',
      emptyOutDir: true,
      sourcemap: true,
      rollupOptions: {
        // empty input, otherwise vite tries to include a non-existing index.html
        input: {},
        output: {
          // set this, if you have dynamic imports to make sure, those chunks are in the correct folder
          chunkFileNames: 'io.ox.public-sector/[name]-[hash].js'
        },
        // rollup-plugin-po2json uses rollup cache during the build process
        cache: true
      }
    },
    server: {
      port: PORT,
      hmr: ENABLE_HMR,
      https: {
        key: process.env.HOST_KEY,
        cert: process.env.HOST_CRT
      }
    },
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    plugins: [
      // proxy settings for Vite dev server
      vitePluginProxy({
        proxy: {
          [`${PROXY_URL.pathname}/api`.replace(/\/+/g, '/')]: {
            target: PROXY_URL.href,
            changeOrigin: true,
            secure: ENABLE_SECURE_PROXY
          },
          '/ajax': {
            target: PROXY_URL.href,
            changeOrigin: true,
            secure: ENABLE_SECURE_PROXY
          },
          '/help': {
            target: PROXY_URL.href,
            changeOrigin: true,
            secure: ENABLE_SECURE_PROXY
          },
          '/meta': {
            target: PROXY_URL.href,
            changeOrigin: true,
            secure: ENABLE_SECURE_PROXY
          },
          '/socket.io/appsuite': {
            target: `wss://${PROXY_URL.host}/socket.io/appsuite`,
            ws: true,
            changeOrigin: true,
            secure: ENABLE_SECURE_PROXY
          }
        },
        httpProxy: ENABLE_HTTP_PROXY && {
          target: PROXY_URL.href,
          port: PROXY_URL.port || 8080
        },
        frontends: FRONTEND_URIS && FRONTEND_URIS.split(',').map(uri => ({ target: uri, secure: ENABLE_SECURE_FRONTENDS }))
      }),
      // serve JSON manifest files and build-time metadata
      vitePluginOxManifests({
        watch: true,
        manifestsAsEntryPoints: true,
        // is just used in dev mode
        includeServerManifests: true,
        // add some metadata here to identify the ui plugin later on
        meta: {
          id: 'public-sector',
          name: 'Public Sector',
          buildDate: new Date().toISOString(),
          commitSha: process.env.CI_COMMIT_SHA,
          version: String(process.env.APP_VERSION || '')
        }
      }),
      // serve external modules starting with "$" (core-ui)
      vitePluginOxExternals({
        prefix: '$'
      }),
      // inject loading of stylesheets in javascript files
      vitePluginOxCss(),
      // localizations (PO files via gettext)
      gettextPlugin({
        poFiles: 'src/i18n/*.po',
        outFile: 'ox.pot',
        defaultDictionary: 'io.ox.public-sector',
        defaultLanguage: 'de_DE'
      }),
      // copy existing assets from source directory that are not imported in source code
      rollupPluginCopy({
        targets: [
          { src: './src/io.ox.public-sector/*.svg', dest: 'public/io.ox.public-sector/' },
          {
            src: './src/io.ox.public-sector/navigation/*.svg',
            dest: 'public/io.ox.public-sector/navigation/'
          }
        ],
        hook: 'buildStart'
      })
    ]
  }
})
