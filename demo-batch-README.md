# UIT-Go RabbitMQ Demo Scripts (Batch Files)

This directory contains pure Windows batch file demonstrations to showcase the RabbitMQ message flow in the UIT-Go ride-sharing platform. No PowerShell dependencies required.

## Available Demo Scripts

### 1. Quick Demo (`demo-rabbitmq-quick.bat`)
**Duration:** ~30 seconds  
**Purpose:** Fast verification that RabbitMQ integration is working

**Features:**
- Checks current queue status via RabbitMQ Management API
- Performs one real driver location update via REST API
- Shows updated queue status
- Confirms message publishing is working

**Usage:**
```cmd
demo-rabbitmq-quick.bat
```

### 2. Full Demo (`demo-rabbitmq-flow.bat`)
**Duration:** 2-3 minutes  
**Purpose:** Comprehensive demonstration of all RabbitMQ flows

**Features:**
- Driver location update flow simulation
- Trip request and driver matching flow
- Status updates and notifications flow
- Queue monitoring and management info

**Usage:**
```cmd
# Normal speed demo
demo-rabbitmq-flow.bat

# Fast demo with health check skip
demo-rabbitmq-flow.bat /fast /skiphealth

# Custom delay (5 seconds between sections)
demo-rabbitmq-flow.bat /delay 5
```

**Parameters:**
- `/skiphealth`: Skip initial service health verification
- `/fast`: Run with minimal delays (1 second between sections)
- `/delay N`: Custom delay in seconds between demo sections (default: 3)

### 3. Interactive Menu (`demo-rabbitmq.bat`)
**Purpose:** Easy-to-use batch file with menu options

**Features:**
- Interactive menu to choose demo type
- No command-line parameters needed
- Pure batch file execution

**Usage:**
```cmd
demo-rabbitmq.bat
```

## Prerequisites

1. **Services Running:** Ensure all services are up via Docker Compose:
   ```cmd
   docker compose -f infra/docker-compose.yml up -d
   ```

2. **curl Available:** The batch files use `curl` for API calls (included in Windows 10/11)

3. **Test Data:** The scripts automatically reference existing test driver data

4. **RabbitMQ Access:** RabbitMQ Management UI should be accessible at:
   - URL: http://localhost:15672
   - Username: guest
   - Password: guest

## What Gets Demonstrated

### Message Flows
1. **Driver Location Updates**
   - REST API → Database → Event → RabbitMQ → Queue
   - Queue: `driver.location.updates`
   - Exchange: `driver.events`
   - Routing Key: `driver.location.updated`

2. **Trip Request Flow**
   - Trip creation → Driver matching → Assignment
   - Queue: `trip.created.queue`
   - Exchange: `trip.events`

3. **Status Updates**
   - Driver status changes → Notifications
   - Queue: `driver.status.changes`
   - Exchange: `driver.events`

### Technical Components
- **REST API Integration:** Direct curl calls to driver service endpoints
- **RabbitMQ Management API:** Queue status monitoring via HTTP API
- **Message Flow Simulation:** Shows complete event-driven workflows
- **Real-time Testing:** Actual API calls trigger real RabbitMQ messages

## Expected Output

### Quick Demo
```
📊 Current RabbitMQ Queue Status:
   ✅ RabbitMQ Management API is accessible
   🟢 driver.location.updates - Queue exists

📍 Testing Driver Location Update...
   ✅ Driver Service is accessible
   ✅ Location update successful!
   📤 RabbitMQ message published to driver.location.updates

🎯 Demo Summary:
   ✓ Driver service REST API connectivity verified
   ✓ RabbitMQ Management API accessibility confirmed
```

### Full Demo
Shows complete message flow scenarios with:
- Simulated driver location updates with JSON payloads
- Trip request and driver matching workflows  
- Status update sequences (DRIVER_ASSIGNED → DRIVER_ARRIVED → TRIP_STARTED → TRIP_COMPLETED)
- Queue monitoring and management interface information

## System Requirements

- **Windows:** Windows 10/11 (for built-in curl support)
- **Services:** Docker containers running (driver-service, RabbitMQ)
- **Network:** Access to localhost ports 8083, 15672
- **No Dependencies:** Pure batch files, no PowerShell or external tools required

## Troubleshooting

### Common Issues

1. **Services Not Running**
   ```
   ❌ Driver Service: UNREACHABLE
   ```
   **Solution:** Start services with `docker compose -f infra/docker-compose.yml up -d`

2. **RabbitMQ Not Accessible**
   ```
   ⚠️ Could not connect to RabbitMQ Management API
   ```
   **Solution:** Check RabbitMQ container is running and port 15672 is accessible

3. **curl Not Found**
   ```
   'curl' is not recognized as an internal or external command
   ```
   **Solution:** Use Windows 10/11 or install curl manually

### Verification Steps

1. **Check Services:**
   ```cmd
   curl http://localhost:8083/api/driver-service/drivers/health
   ```

2. **Check RabbitMQ:**
   ```cmd
   curl -u guest:guest http://localhost:15672/api/queues
   ```

3. **View Service Logs:**
   ```cmd
   docker logs driver-service --tail 20
   ```

## File Structure

```
├── demo-rabbitmq.bat              # Main menu (interactive)
├── demo-rabbitmq-quick.bat        # Quick 30-second demo
├── demo-rabbitmq-flow.bat         # Full 2-3 minute demo
└── demo-batch-README.md           # This documentation
```

## Next Steps

After running the demos:

1. **Explore RabbitMQ Management UI** to see real queue activity
2. **Implement message consumers** to process the queued messages
3. **Add more event types** (trip completion, payments, etc.)
4. **Set up monitoring** for queue depths and processing rates
5. **Implement dead letter queues** for failed message handling

The demos prove that your event-driven architecture is working and ready for building real-time features like live tracking, instant notifications, and automatic trip matching.

## Advantages of Batch File Approach

- **No Dependencies:** Works on any Windows system without PowerShell
- **Simple Deployment:** Single file execution, no script policy concerns
- **Direct API Testing:** Uses curl to make actual REST calls
- **Cross-Compatible:** Works in Command Prompt, PowerShell, or any Windows terminal
- **Easy Integration:** Can be called from CI/CD pipelines or other batch processes