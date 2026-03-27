import { useState } from 'react'
import axios from 'axios'

function App() {
  // Estados para os campos do formulário (Entradas do Usuário)
  const [originCity, setOriginCity] = useState('')
  const [destinationCity, setDestinationCity] = useState('')
  const [weight, setWeight] = useState('')
  const [departureTime, setDepartureTime] = useState('2026-03-30T08:00') // Data padrão no futuro

  // Estados para o controle da tela
  const [tripResult, setTripResult] = useState(null)
  const [loading, setLoading] = useState(false)
  const [statusMessage, setStatusMessage] = useState('')

  // Transforma nome da cidade em Coordenadas (Geocoding)
  const getCoordinates = async (cityName) => {
    try {
      setStatusMessage(`Buscando coordenadas de ${cityName}...`)
      // Chamada para a API gratuita do OpenStreetMap
      const response = await axios.get(
        `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(cityName)}&limit=1`
      )

      if (response.data && response.data.length > 0) {
        const { lat, lon } = response.data[0]
        return { lat: Number.parseFloat(lat), lng: Number.parseFloat(lon) }
      } else {
        throw new Error(`Cidade não encontrada: ${cityName}`)
      }
    } catch (error) {
      throw new Error(`Erro na geocodificação de ${cityName}: ${error.message}`)
    }
  }

  // Orquestra o despacho da viagem
  const handleDispatchTrip = async (e) => {
    e.preventDefault()
    setLoading(true)
    setTripResult(null)
    setStatusMessage('Iniciando processo...')

    try {
      // Obter coordenadas das cidades
      const originCoords = await getCoordinates(originCity)
      const destCoords = await getCoordinates(destinationCity)

      setStatusMessage('Cidades localizadas! Consultando Inteligência Artificial no backend...')

      const payload = {
        originLat: originCoords.lat,
        originLng: originCoords.lng,
        destLat: destCoords.lat,
        destLng: destCoords.lng,
        payloadWeight: Number.parseFloat(weight),
        departureTime: departureTime + ":00"
      }

      const response = await axios.post('http://localhost:8080/api/v1/trips', payload)

      console.log("DADOS ENVIADOS:", payload);
      console.log("RESPOSTA DO BACKEND:", response.data);

      setTripResult(response.data)
      setStatusMessage('Viagem despachada com sucesso!')

    } catch (error) {
      console.error(error)
      setStatusMessage(`❌ Erro: ${error.message}`)
      alert(`Houve um erro no despacho: ${error.message}`)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={styles.container}>
      <header style={styles.header}>
        <h1>Smartroute Dispatcher 🚚</h1>
        <p>Previsão de Chegada com IA e Geocodificação de Cidades</p>
      </header>

      <main style={styles.main}>
        {/* FORMULÁRIO DE ENTRADA */}
        <form onSubmit={handleDispatchTrip} style={styles.form}>
          <h2>Novo Despacho</h2>

          <div style={styles.inputGroup}>
            <label>Cidade de Origem: </label>
            <input type="text" value={originCity} onChange={(e) => setOriginCity(e.target.value)} required placeholder="Ex: São Paulo" />
          </div>

          <div style={styles.inputGroup}>
            <label>Cidade de Destino: </label>
            <input type="text" value={destinationCity} onChange={(e) => setDestinationCity(e.target.value)} required placeholder="Ex: Rio de Janeiro" />
          </div>

          <div style={styles.row}>
            <div style={styles.inputGroup}>
              <label>Carga (Toneladas): </label>
              <input type="number" step="0.1" value={weight} onChange={(e) => setWeight(e.target.value)} required />
            </div>

            <div style={styles.inputGroup}>
              <label>Horário de Saída: </label>
              <input type="datetime-local" value={departureTime} onChange={(e) => setDepartureTime(e.target.value)} required />
            </div>
          </div>

          <button type="submit" disabled={loading} style={loading ? styles.buttonDisabled : styles.button}>
            {loading ? "Processando..." : "Calcular Rota com IA"}
          </button>

          {statusMessage && <p style={styles.status}>{statusMessage}</p>}
        </form>

        {tripResult && (
          <div style={styles.resultCard}>
            <h3 style={styles.resultTitle}>✅ Viagem Criada no Banco de Dados</h3>

            <div style={styles.resultDetails}>
              <p><strong>De:</strong> {originCity} ({tripResult.originLat.toFixed(4)}, {tripResult.originLng.toFixed(4)})</p>
              <p><strong>Para:</strong> {destinationCity} ({tripResult.destLat.toFixed(4)}, {tripResult.destLng.toFixed(4)})</p>
              <p><strong>Peso:</strong> {tripResult.payloadWeight} Toneladas</p>
            </div>

            <div style={styles.aiPanel}>
              <h4>🧠 Previsão da Inteligência Artificial:</h4>
              <p style={styles.etaText}>
                Chegada estimada: <strong>{new Date(tripResult.predictedArrival).toLocaleString()}</strong>
              </p>
              <p style={styles.versionText}>Modelo: Random Forest v1.0 | Status: {tripResult.status}</p>
            </div>
          </div>
        )}
      </main>
    </div>
  )
}

// Estilos CSS rápidos em JavaScript
const styles = {
  container: { fontFamily: 'Segoe UI, Tahoma, Geneva, Verdana, sans-serif', backgroundColor: '#f4f7f6', minHeight: '100vh', color: '#333' },
  header: { textAlign: 'center', padding: '2rem', backgroundColor: '#fff', boxShadow: '0 2px 4px rgba(0,0,0,0.1)' },
  main: { display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '2rem' },
  form: { backgroundColor: '#fff', padding: '2rem', borderRadius: '8px', boxShadow: '0 4px 6px rgba(0,0,0,0.1)', width: '100%', maxWidth: '500px', marginBottom: '2rem' },
  inputGroup: { marginBottom: '1rem', display: 'flex', flexDirection: 'column' },
  row: { display: 'flex', gap: '1rem' },
  status: { marginTop: '10px', fontSize: '0.9rem', color: '#666', fontStyle: 'italic', textAlign: 'center' },
  button: { width: '100%', padding: '12px', fontSize: '1.1rem', backgroundColor: '#007bff', color: '#fff', border: 'none', borderRadius: '4px', cursor: 'pointer', transition: 'background 0.3s' },
  buttonDisabled: { width: '100%', padding: '12px', fontSize: '1.1rem', backgroundColor: '#ccc', color: '#fff', border: 'none', borderRadius: '4px', cursor: 'not-allowed' },
  resultCard: { backgroundColor: '#fff', padding: '2rem', borderRadius: '8px', borderLeft: '5px solid #28a745', boxShadow: '0 4px 6px rgba(0,0,0,0.1)', width: '100%', maxWidth: '500px' },
  resultTitle: { color: '#28a745', marginTop: 0 },
  resultDetails: { marginBottom: '1.5rem', borderBottom: '1px solid #eee', paddingBottom: '1rem' },
  aiPanel: { backgroundColor: '#e9f7ef', padding: '1rem', borderRadius: '4px' },
  etaText: { fontSize: '1.3rem', color: '#155724', margin: '10px 0' },
  versionText: { fontSize: '0.8rem', color: '#155724', opacity: 0.7, margin: 0 }
}

export default App
